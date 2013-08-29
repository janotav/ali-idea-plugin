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

package com.hp.alm.ali.idea.action.task;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Set;

public class TaskDeleteAction extends EntityAction {

    public TaskDeleteAction() {
        super("Delete", "Delete task", IconLoader.getIcon("/actions/cancel.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("project-task");
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, final Entity entity) {
        if(Messages.showYesNoDialog("Do you really want to delete this task?", "Confirmation", null) == Messages.YES) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    EntityService entityService = project.getComponent(EntityService.class);
                    if(entityService.deleteEntity(entity)) {
                        // make sure item values are propagated
                        entityService.getEntityAsync(new EntityRef("release-backlog-item", Integer.valueOf(entity.getPropertyValue("release-backlog-item-id"))), null);
                    }
                }
            });
        }
    }
}
