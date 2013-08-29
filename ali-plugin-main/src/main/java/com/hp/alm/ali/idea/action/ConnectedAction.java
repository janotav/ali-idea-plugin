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

import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;

public abstract class ConnectedAction extends AnAction {

    public ConnectedAction(String name, String description, Icon icon) {
        super(name, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(isConnected(e));
    }

    public static boolean isConnected(AnActionEvent e) {
        Project project = getEventProject(e);
        return project != null && project.getComponent(RestService.class).getServerTypeIfAvailable().isConnected();
    }
}
