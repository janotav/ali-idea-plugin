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

import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.WeakListeners;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskListenerAdapter;
import com.intellij.tasks.TaskManager;
import com.intellij.util.ui.UIUtil;

public class TaskManagerIntegration {
    private Project project;
    private TaskManager taskManager;
    private EntityService entityService;
    private WeakListeners<Listener> listeners;
    volatile private boolean fireEvent;

    public TaskManagerIntegration(Project project, EntityService entityService) {
        this.project = project;
        this.entityService = entityService;
        listeners = new WeakListeners<Listener>();
        fireEvent = true;

        this.taskManager = project.getComponent(TaskManager.class);
        if(taskManager != null) {
            // no task manager in default project (genesis action)
            taskManager.addTaskListener(new TaskListenerAdapter() {
                @Override
                public void taskActivated(LocalTask task) {
                    if(fireEvent) {
                        final Entity entity;
                        if ((task.getRepository() instanceof HpAlmRepository || (task.getRepository() == null && EntityRef.isEntityRef(task.getId())))) {
                            EntityRef ref = new EntityRef(task.getId());
                            entity = new Entity(ref.type, ref.id);
                        } else {
                            entity = null;
                        }
                        listeners.fire(new WeakListeners.Action<Listener>() {
                            @Override
                            public void fire(Listener listener) {
                                listener.taskEntityActivated(entity);
                            }
                        });
                    }
                }
            });
        }
    }

    public void activate(Entity entity) {
        if(entity == null) {
            activateDefaultTask();
        } else {
            HpAlmTask task = new HpAlmTask(project, entity);
            if(!task.isInitialized()) {
                entityService.getEntityAsync(new EntityRef(entity), new EntityAdapter() {
                    @Override
                    public void entityLoaded(final Entity entity, Event event) {
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            public void run() {
                                // throws exception when not executed in dispatcher (10.5.2)
                                activateTask(new HpAlmTask(project, entity));
                            }
                        });
                    }
                });
            } else {
                activateTask(task);
            }
        }
    }

    Task _getDefaultTask() {
        for(LocalTask task: taskManager.getLocalTasks()) {
            if("Default".equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    private void activateDefaultTask() {
        Task defaultTask = _getDefaultTask();
        if(defaultTask != null) {
            activateTask(defaultTask);
        }
    }

    private void activateTask(Task task) {
        fireEvent = false;
        try {
            taskManager.activateTask(task, false, false);
        } finally {
            fireEvent = true;
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static interface Listener {

        void taskEntityActivated(Entity entity);

    }
}
