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

package com.hp.alm.ali.idea.action.taskboard;

import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.ui.editor.UserStoryEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

public class TaskBoardNewStoryAction extends SprintScopedAction {

    public TaskBoardNewStoryAction() {
        super("New user story", "Create new user story", IconLoader.getIcon("/fileTypes/text.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project != null) {
            SprintService sprintService = project.getComponent(SprintService.class);
            int releaseId = sprintService.getRelease().getId();
            int sprintId = sprintService.getSprint().getId();
            int teamId = sprintService.getTeam().getId();
            new UserStoryEditor(project, releaseId, sprintId, teamId).execute();
        }
    }
}
