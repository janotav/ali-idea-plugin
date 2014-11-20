/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.HorizonStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.AgmUrlService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BrowserAction extends EntityAction {

    private static Map<String, Launcher> entityTypes;
    static {
        entityTypes = new HashMap<String, Launcher>();
        entityTypes.put("build-artifact", new PropertyLauncher("artifact-url"));
        entityTypes.put("defect", new BacklogLauncher());
        entityTypes.put("requirement", new BacklogLauncher());
        entityTypes.put("build-instance", new BuildDetailLauncher());
        entityTypes.put("changeset", new ChangesetLauncher());
    }

    public BrowserAction() {
        super("Web Browser", "View in web browser", IconLoader.getIcon("/general/web.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return entityTypes.keySet();
    }

    @Override
    protected void update(AnActionEvent event, Project project, Entity entity) {
        boolean enabled = entityTypes.get(entity.getType()).isEnabled(project);
        event.getPresentation().setEnabled(enabled);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        entityTypes.get(entity.getType()).launch(project, entity);
    }

    private static class ChangesetLauncher extends AgmLauncher {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getChangesetDetailUrl(entity, tenantId);
        }
    }

    private static class BuildDetailLauncher extends AgmLauncher {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getBuildDetailUrl(entity, tenantId);
        }
    }

    private static class BacklogLauncher extends AgmLauncher {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getBacklogUrl(entity, tenantId);
        }
    }

    private static abstract class AgmLauncher implements Launcher {

        @Override
        public void launch(Project project, Entity entity) {
            String tenantId = getTenantId(project);
            if (tenantId != null) {
                String entityUrl = getUrl(project, entity, tenantId);
                BrowserUtil.launchBrowser(entityUrl);
            }
        }

        @Override
        public boolean isEnabled(Project project) {
            return project.getComponent(RestService.class).getServerTypeIfAvailable() == ServerType.AGM &&
                    getTenantId(project) != null;
        }

        protected abstract String getUrl(Project project, Entity entity, String tenantId);

        private String getTenantId(Project project) {
            return project.getComponent(HorizonStrategy.class).getTenantId();
        }
    }

    private static class PropertyLauncher implements Launcher {
        private String property;

        public PropertyLauncher(String property) {
            this.property = property;
        }

        @Override
        public void launch(Project project, Entity entity) {
            project.getComponent(EntityService.class).getEntityAsync(new EntityRef(entity), new EntityAdapter() {
                @Override
                public void entityLoaded(Entity entity, Event event) {
                    BrowserUtil.launchBrowser(entity.getPropertyValue(property));
                }
            });
        }

        @Override
        public boolean isEnabled(Project project) {
            return true;
        }
    }

    private interface Launcher {

        void launch(Project project, Entity entity);

        boolean isEnabled(Project project);

    }
}
