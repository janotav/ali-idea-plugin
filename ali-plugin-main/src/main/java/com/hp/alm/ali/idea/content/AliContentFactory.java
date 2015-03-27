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

package com.hp.alm.ali.idea.content;

import com.hp.alm.ali.idea.content.devmotive.DevMotive;
import com.hp.alm.ali.idea.content.devmotive.DevMotivePanel;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.content.settings.SettingsContent;
import com.hp.alm.ali.idea.content.detail.EntityDetail;
import com.hp.alm.ali.idea.content.detail.HasEntity;
import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class AliContentFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_MAIN = "HP ALI";
    public static final String TOOL_WINDOW_DETAIL = "ALI Detail";

    @Override
    public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        SettingsContent settings = SettingsContent.getInstance();
        Content content = ContentFactory.SERVICE.getInstance().createContent(settings.create(project), settings.getName(), false);
        toolWindow.getContentManager().addContent(content);

        final RestService restService = project.getComponent(RestService.class);
        toolWindow.getComponent().addPropertyChangeListener("ancestor", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                if(pce.getOldValue() == null && pce.getNewValue() != null) {
                    if(toolWindow.getContentManager().getContentCount() > 1) {
                        restService.checkConnectivity();
                    }
                } else if(pce.getOldValue() != null && pce.getNewValue() == null) {
                    restService.expireConnectivityError();
                }
            }
        });
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void selectionChanged(ContentManagerEvent event) {
                project.getComponent(AliProjectConfiguration.class).setSelectedContent(event.getContent().getTabName());
            }

            @Override
            public void contentRemoveQuery(ContentManagerEvent event) {
                if (!(event.getContent().getComponent() instanceof CloseableContent)) {
                    event.consume();
                }
            }
        });
        new AliContentManager(toolWindow, project);
        ProjectManager.getInstance().addProjectManagerListener(project, new MyProjectListener(restService));
    }

    public static void loadDetails(Project project) {
        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
        List<EntityRef> refs = conf.getDetails().getRefs();
        if(!refs.isEmpty()) {
            ToolWindow toolWindow = getOrCreateDetailToolWindow(project);
            EntityRef selectedRef = conf.getDetails().getSelectedRef();
            if(refs.contains(selectedRef)) {
                // add selected as first to avoid loading more than one detail during initialization
                addEntity(project, toolWindow, selectedRef.toEntity(), -1);
            }
            for(int i = 0; i < refs.size(); i++) {
                EntityRef ref = refs.get(i);
                if(!ref.equals(selectedRef)) {
                    addEntity(project, toolWindow, ref.toEntity(), i);
                }
            }
        }
    }

    public static void loadDetail(Project project, Entity entity, boolean show, boolean select) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        ToolWindow toolWindow = getOrCreateDetailToolWindow(project);
        Content content = addEntity(project, toolWindow, entity, -1);
        if(show) {
            toolWindow.show(null);
        }
        if(select) {
            AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
            conf.getDetails().setSelectedRef(new EntityRef(entity));
            toolWindow.getContentManager().setSelectedContent(content);
        }
    }

    public static DevMotivePanel addDevMotiveContent(Project project, VirtualFile file, boolean select) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        ToolWindowManager toolWindowManager = project.getComponent(ToolWindowManager.class);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_MAIN);
        ContentManager contentManager = toolWindow.getContentManager();

        Content content = findDevMotiveContent(toolWindow, file);
        if (content == null) {
            contentManager = toolWindow.getContentManager();
            int idx = contentManager.getContentCount();
            DevMotivePanel devMotivePanel = new DevMotivePanel(project, file);
            content = ContentFactory.SERVICE.getInstance().createContent(devMotivePanel, "Dev: " + file.getName(), false);
            contentManager.addContent(content, idx);
        }
        if (select) {
            contentManager.setSelectedContent(content);
        }
        return (DevMotivePanel) content.getComponent();
    }

    public static Content findDevMotiveContent(ToolWindow toolWindow, VirtualFile file) {
        for(Content content: toolWindow.getContentManager().getContents()) {
            if(isDevMotiveContentOf(content, file)) {
                return content;
            }
        }
        return null;
    }

    public static boolean isDevMotiveContentOf(Content content, VirtualFile file) {
        if(content.getComponent() instanceof DevMotive) {
            if(file.equals(((DevMotive) content.getComponent()).getFile())) {
                return true;
            }
        }
        return false;
    }

    private static Content addEntity(Project project, ToolWindow toolWindow, final Entity entity, int idx) {
        Content content = findContent(toolWindow, entity);
        if(content == null) {
            EntityDetail entityDetail = new EntityDetail(project, entity);
            EntityRef ref = new EntityRef(entity);
            project.getComponent(AliProjectConfiguration.class).getDetails().addRef(ref);
            content = ContentFactory.SERVICE.getInstance().createContent(entityDetail, ref.toString(), false);
            final Content fContent = content;
            EntityLabelService entityLabelService = project.getComponent(EntityLabelService.class);
            entityLabelService.loadEntityLabelAsync(entity.getType(), new AbstractCachingService.DispatchCallback<String>() {
                @Override
                public void loaded(String entityLabel) {
                    fContent.setDisplayName(entityLabel + " #" + entity.getId());
                }
            });
            if(idx < 0) {
                // ToolWindowHeadlessManager does not accept -1
                idx = toolWindow.getContentManager().getContentCount();
            }
            toolWindow.getContentManager().addContent(content, idx);
            toolWindow.getContentManager().addContentManagerListener(entityDetail);
            entityDetail.updateSelection(toolWindow.getContentManager().getSelectedContent());
        }
        return content;
    }

    public static Content findContent(ToolWindow toolWindow, Entity entity) {
        for(Content content: toolWindow.getContentManager().getContents()) {
            if(isContentOf(content, entity)) {
                return content;
            }
        }
        return null;
    }

    public static boolean isContentOf(Content content, Entity entity) {
        if(content.getComponent() instanceof HasEntity) {
            if(entity.equals(((HasEntity) content.getComponent()).getEntity())) {
                return true;
            }
        }
        return false;
    }

    public static ToolWindow getDetailToolWindow(Project project) {
        ToolWindowManager toolWindowManager = project.getComponent(ToolWindowManager.class);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_DETAIL);
    }

    private static ToolWindow getOrCreateDetailToolWindow(Project project) {
        ToolWindow toolWindow = getDetailToolWindow(project);
        if(toolWindow == null) {
            ToolWindowManager toolWindowManager = project.getComponent(ToolWindowManager.class);
            toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_DETAIL, true, ToolWindowAnchor.RIGHT);
            toolWindow.setToHideOnEmptyContent(true);
            toolWindow.setIcon(IconLoader.getIcon("/ali_icon_13x13.png"));
            toolWindow.getContentManager().addContentManagerListener(project.getComponent(EntityEditManager.class));
        }
        return toolWindow;
    }

    private static class MyProjectListener extends ProjectManagerAdapter {

        private RestService restService;

        public MyProjectListener(RestService restService) {
            this.restService = restService;
        }

        public void projectClosed(Project closedProject) {
            RestService.logout(restService.getRestClient());
            ProjectManager.getInstance().removeProjectManagerListener(closedProject, this);
        }
    }

}
