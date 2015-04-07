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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.content.devmotive.DevMotive;
import com.hp.alm.ali.idea.model.ServerStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.util.ArrayList;

public class AliContentManager implements ServerTypeListener {

    private ToolWindow toolWindow;
    private Project project;
    private boolean connected;

    public AliContentManager(ToolWindow toolWindow, Project project) {
        this.toolWindow = toolWindow;
        this.project = project;
        connected = false;

        RestService restService = project.getComponent(RestService.class);
        restService.addServerTypeListener(this, false);
        if(restService.getServerTypeIfAvailable().isConnected()) {
            connectedTo(restService.getServerTypeIfAvailable());
        } else {
            restService.checkConnectivity();
        }
    }

    @Override
    public void connectedTo(final ServerType serverType) {
        if(serverType.isConnected()) {
            synchronized (this) {
                if(!connected) {
                    connected = true;
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        public void run() {
                            AliContentFactory.loadDetails(project);
                        }
                    });
                }
            }

            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    Content[] contents = toolWindow.getContentManager().getContents();
                    ServerStrategy serverStrategy = project.getComponent(serverType.getClazz());
                    String selectedContent = project.getComponent(AliProjectConfiguration.class).getSelectedContent();
                    ArrayList<AliContent> supportedContent = new ArrayList<AliContent>(serverStrategy.getSupportedContent());
                    for (Content content : contents) {
                        boolean found = false;
                        for(int i = 0; i < supportedContent.size(); i++) {
                            AliContent aliContent = supportedContent.get(i);
                            if (aliContent != null && content.getTabName().equals(aliContent.getName())) {
                                supportedContent.set(i, null);
                                found = true;
                                break;
                            }
                        }
                        if (!found && !(content.getComponent() instanceof DevMotive)) {
                            toolWindow.getContentManager().removeContent(content, true);
                        }
                    }
                    for(int i = 0; i < supportedContent.size(); i++) {
                        AliContent aliContent = supportedContent.get(i);
                        if(aliContent == null) {
                            continue;
                        }

                        JComponent comp = aliContent.create(project);
                        Content content = ContentFactory.SERVICE.getInstance().createContent(comp, aliContent.getName(), false);
                        toolWindow.getContentManager().addContent(content, i);
                        if(content.getTabName().equals(selectedContent)) {
                            toolWindow.getContentManager().setSelectedContent(content);
                        }
                    }
                }
            });
        }
    }

    public static void assertNotDispatchThread() {
        if(SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
    }
}
