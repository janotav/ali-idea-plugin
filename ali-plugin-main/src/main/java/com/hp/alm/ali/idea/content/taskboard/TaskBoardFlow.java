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

package com.hp.alm.ali.idea.content.taskboard;

import com.hp.alm.ali.idea.cfg.TaskBoardConfiguration;
import com.hp.alm.ali.idea.content.AliContentManager;
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.hp.alm.ali.idea.services.EntityService;
import com.intellij.util.ui.UIUtil;

import java.util.Arrays;
import java.util.Collections;

public class TaskBoardFlow {

    private EntityService entityService;
    private ActiveItemService activeItemService;
    private TaskBoardConfiguration taskBoardConfiguration;

    public TaskBoardFlow(EntityService entityService, ActiveItemService activeItemService, TaskBoardConfiguration taskBoardConfiguration) {
        this.entityService = entityService;
        this.activeItemService = activeItemService;
        this.taskBoardConfiguration = taskBoardConfiguration;
    }

    public void onTaskUpdated(final Entity task) {
        entityService.requestCachedEntity(new EntityRef("release-backlog-item", Integer.valueOf(task.getPropertyValue("release-backlog-item-id"))), Arrays.asList("entity-id", "entity-type"), new EntityAdapter() {
            @Override
            public void entityLoaded(Entity entity, Event event) {
                onTaskUpdated(task, entity);
            }
        });
    }

    public void onTaskUpdated(Entity task, Entity backlogItem) {
        AliContentManager.assertNotDispatchThread();

        String taskStatus = task.getPropertyValue("status");
        if (TaskPanel.TASK_COMPLETED.equals(taskStatus)) {
            String tasksCompletedStatus = taskBoardConfiguration.getTasksCompletedStatus();
            EntityRef workItem = new EntityRef(backlogItem.getPropertyValue("entity-type"), Integer.valueOf(backlogItem.getPropertyValue("entity-id")));
            boolean deactivate = taskBoardConfiguration.isDeactivateItem() &&
                    activeItemService.getActiveItem() != null &&
                    activeItemService.getActiveItem().equals(workItem);
            if (tasksCompletedStatus != null || deactivate) {
                // when task moves to completed, we need to see if there are incomplete tasks remaining
                EntityQuery query = new EntityQuery("project-task");
                query.addColumn("id", 1);
                query.setValue("status", "<> Completed");
                query.setValue("release-backlog-item-id", String.valueOf(backlogItem.getId()));
                query.setPropertyResolved("status", true);
                EntityList incompleteTasks = entityService.query(query);
                if(incompleteTasks.isEmpty()) {
                    if (tasksCompletedStatus != null) {
                        // move backlog item to target state
                        backlogItem.setProperty("status", tasksCompletedStatus);
                        entityService.updateEntity(backlogItem, Collections.singleton("status"), false, true);
                    }
                    if (deactivate) {
                        // deactivate work item
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            @Override
                            public void run() {
                                activeItemService.activate(null, true, false);
                            }
                        });
                    }
                }
            }
        } else if (TaskPanel.TASK_IN_PROGRESS.equals(taskStatus)) {
            // when task moves to progress, backlog item must move to progress too
            backlogItem.setProperty("status", taskStatus);
            entityService.updateEntity(backlogItem, Collections.singleton("status"), false, true);
        } else if(BacklogItemPanel.ITEM_DONE.equals(backlogItem.getProperty("status"))) {
            // when task moves to new and backlog item was completed, move it to progress
            backlogItem.setProperty("status", TaskPanel.TASK_IN_PROGRESS);
            entityService.updateEntity(backlogItem, Collections.singleton("status"), false, true);
        } else {
            // make sure the backlog item is reloaded
            entityService.getEntityAsync(new EntityRef(backlogItem), null);
        }
    }
}
