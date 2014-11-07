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

package com.hp.alm.ali.idea.entity.queue;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityStatusIndicator;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.util.LinkedList;

public class QueryQueue {

    private EntityService entityService;
    private EntityStatusIndicator status;
    private boolean updateStatus;
    private QueryTarget target;
    final private LinkedList<Object> queue = new LinkedList<Object>();

    // serialized query execution: if another query comes before waiting query
    // is started, discard the waiting query (thus queue never grows beyond one item)
    // the redo parameter passed into the runnable can be used as a shortcut to reschedule the action,
    // e.g. if the execution fails

    public QueryQueue(Project project, EntityStatusIndicator status, boolean updateStatus, QueryTarget target) {
        this.status = status;
        this.updateStatus = updateStatus;
        this.target = target;
        entityService = project.getComponent(EntityService.class);
    }

    public void query(final EntityQuery query) {
        final Object token = new Object();
        synchronized(queue) {
            queue.clear();
            queue.add(token);
        }
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                synchronized (QueryQueue.this) {
                    synchronized (queue) {
                        if (queue.isEmpty() || !queue.getFirst().equals(token)) {
                            return;
                        } else {
                            queue.removeFirst();
                        }
                    }
                    if(query == null) {
                        status.clear();
                        target.handleResult(EntityList.empty());
                        return;
                    }
                    status.loading();
                    Runnable redo = new Runnable() {
                        @Override
                        public void run() {
                            query(query);
                        }
                    };
                    try {
                        EntityList list = entityService.query(query);
                        if(updateStatus) {
                            status.loaded(list, redo);
                        }
                        target.handleResult(list);
                    } catch (Exception e) {
                        status.info("Failed to load data", e, redo, null);
                    }
                }
            }
        });
    }

    public void setStatusIndicator(EntityStatusIndicator status) {
        this.status = status;
    }
}
