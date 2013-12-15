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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.IdeaCompatibility;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;

public class Tasks129Impl implements TasksApi {

    private Project project;

    public Tasks129Impl(Project project, IdeaCompatibility ideaCompatibility) {
        this.project = project;
        ideaCompatibility.register(TasksApi.class, Tasks129Impl.class, 129);
    }

    @Override
    public void activateTask(Task task) {
        project.getComponent(TaskManager.class).activateTask(task, false, false);
    }
}
