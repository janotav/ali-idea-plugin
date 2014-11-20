// (C) Copyright 2003-2014 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.HorizonStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.AgmUrlService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CopyUrlToClipboardAction extends EntityAction {

    private static Map<String, UrlCopy> entityTypes;
    static {
        entityTypes = new HashMap<String, UrlCopy>();
        entityTypes.put("defect", new BacklogUrlCopy());
        entityTypes.put("requirement", new BacklogUrlCopy());
        entityTypes.put("build-instance", new BuildDetailUrlCopy());
        entityTypes.put("changeset", new ChangesetDetailUrlCopy());
    }

    public CopyUrlToClipboardAction() {
        super("Copy Web URL", "Copy Web URL to Clipboard", IconLoader.getIcon("/actions/copy.png"));
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
        entityTypes.get(entity.getType()).copyURL(project, entity);
    }

    private static class ChangesetDetailUrlCopy extends AgmUrlCopy {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getChangesetDetailUrl(entity, tenantId);
        }
    }

    private static class BuildDetailUrlCopy extends AgmUrlCopy {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getBuildDetailUrl(entity, tenantId);
        }
    }

    private static class BacklogUrlCopy extends AgmUrlCopy {

        @Override
        protected String getUrl(Project project, Entity entity, String tenantId) {
            return project.getComponent(AgmUrlService.class).getBacklogUrl(entity, tenantId);
        }
    }

    private abstract static class AgmUrlCopy implements UrlCopy {

        @Override
        public void copyURL(Project project, Entity entity) {
            String tenantId = getTenantId(project);
            if (tenantId != null) {
                String entityUrl = getUrl(project, entity, tenantId);
                CopyPasteManager.getInstance().setContents(new StringSelection(entityUrl));
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

    private interface UrlCopy {

        void copyURL(Project project, Entity entity);

        boolean isEnabled(Project project);

    }
}
