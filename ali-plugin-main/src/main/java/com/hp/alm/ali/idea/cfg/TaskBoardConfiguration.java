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
        name = "TaskboardConfiguration",
        storages = { @Storage(id = "default",file = "$WORKSPACE_FILE$") }
)
public class TaskBoardConfiguration implements PersistentStateComponent<Element> {

    public static final String ALL_STATUSES = "<all>";

    private String assignedTo;
    private boolean showDefects = true;
    private boolean showUserStories = true;
    private boolean showBlocked = true;
    private String showStatuses = ALL_STATUSES;
    private String filter;
    private String tasksCompletedStatus;

    public boolean isShowDefects() {
        return showDefects;
    }

    public void setShowDefects(boolean showDefects) {
        this.showDefects = showDefects;
    }

    public boolean isShowUserStories() {
        return showUserStories;
    }

    public void setShowUserStories(boolean showUserStories) {
        this.showUserStories = showUserStories;
    }

    public boolean isShowBlocked() {
        return showBlocked;
    }

    public void setShowBlocked(boolean showBlocked) {
        this.showBlocked = showBlocked;
    }

    public String getShowStatuses() {
        return showStatuses;
    }

    public void setShowStatuses(String statuses) {
        this.showStatuses = statuses;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getTasksCompletedStatus() {
        return tasksCompletedStatus;
    }

    public void setTasksCompletedStatus(String status) {
        this.tasksCompletedStatus = status;
    }

    @Override
    public Element getState() {
        Element element = new Element(getClass().getSimpleName());
        if(assignedTo != null) {
            element.setAttribute("assigned-to", assignedTo);
        }
        if(filter != null) {
            element.setAttribute("filter", filter);
        }
        element.setAttribute("showUserStories", String.valueOf(showUserStories));
        element.setAttribute("showDefects", String.valueOf(showDefects));
        element.setAttribute("showBlocked", String.valueOf(showBlocked));
        element.setAttribute("showStatuses", showStatuses);
        if (tasksCompletedStatus != null) {
            element.setAttribute("tasksCompletedStatus", tasksCompletedStatus);
        }
        return element;
    }

    @Override
    public void loadState(Element element) {
        assignedTo = element.getAttributeValue("assigned-to");
        filter = element.getAttributeValue("filter");
        showUserStories = Boolean.parseBoolean(element.getAttributeValue("showUserStories", "true"));
        showDefects = Boolean.parseBoolean(element.getAttributeValue("showDefects", "true"));
        showBlocked = Boolean.parseBoolean(element.getAttributeValue("showBlocked", "true"));
        showStatuses = element.getAttributeValue("showStatuses", ALL_STATUSES);
        tasksCompletedStatus = element.getAttributeValue("tasksCompletedStatus");
    }
}
