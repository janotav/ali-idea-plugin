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

package com.hp.alm.ali.idea.cfg;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;

@State(
        name = "WorkspaceConfiguration",
        storages = { @Storage(id = "default",file = "$WORKSPACE_FILE$") }
)
public class WorkspaceConfiguration implements PersistentStateComponent<Element> {

    private Integer workspaceId;
    private String workspaceName;

    @Override
    public Element getState() {
        Element element = new Element(getClass().getSimpleName());
        if(workspaceName != null) {
            element.setAttribute("workspace-name", workspaceName);
        }
        if(workspaceId != null) {
            element.setAttribute("workspace-id", String.valueOf(workspaceId));
        }
        return element;
    }

    @Override
    public void loadState(Element element) {
        workspaceName = element.getAttributeValue("workspace-name");
        try {
            workspaceId = Integer.valueOf(element.getAttributeValue("workspace-id"));
        } catch (NumberFormatException e) {
            // no workspace
        }
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}
