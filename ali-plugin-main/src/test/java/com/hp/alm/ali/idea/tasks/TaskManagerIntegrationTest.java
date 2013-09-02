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

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskListenerAdapter;
import com.intellij.tasks.TaskManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TaskManagerIntegrationTest extends IntellijTest {

    private TaskManagerIntegration taskManagerIntegration;
    private TaskManager taskManager;
    private ActiveItemService activeItemService;

    @Before
    public void preClean() {
        taskManagerIntegration = getComponent(TaskManagerIntegration.class);
        taskManager = getComponent(TaskManager.class);
        activeItemService = getComponent(ActiveItemService.class);

        // prevent detail opening when manipulating tasks in this test
        taskManagerIntegration.removeListener(activeItemService);
    }

    @After
    public void postClean() {
        taskManagerIntegration.addListener(activeItemService);
    }

    public TaskManagerIntegrationTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testActivate() {
        Entity defect = new Entity("defect", 1);
        defect.setProperty("name", "Name...");
        defect.setProperty("description", "Description...");
        defect.setProperty("dev-comments", "Comments...");
        defect.setProperty("last-modified", "2012-12-12 12:12:12");
        defect.setProperty("creation-time", "2012-12-12");
        taskManagerIntegration.activate(defect);
        Assert.assertEquals("defect #1: Name...", taskManager.getActiveTask().getPresentableName());
    }

    @Test
    public void testActivate_uninitialized() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        Entity defect = new Entity("defect", 86);
        handler.async();
        taskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskActivated(final LocalTask localTask) {
                taskManager.removeTaskListener(this);
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defect #86: somewhat", localTask.getPresentableName());
                    }
                });
            }
        });
        taskManagerIntegration.activate(defect);
    }

    @Test
    public void testActivate_default() {
        testActivate();

        taskManagerIntegration.activate(null);
        Assert.assertEquals("Default task", taskManager.getActiveTask().getPresentableName());
    }

    @Test
    public void testTaskManagerEvent() {
        activateDefault();

        handler.async();
        taskManagerIntegration.addListener(new TaskManagerIntegration.Listener() {
            @Override
            public void taskEntityActivated(final Entity entity) {
                taskManagerIntegration.removeListener(this);
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defect", entity.getType());
                        Assert.assertEquals(85, entity.getId());
                    }
                });
            }
        });

        HpAlmTask task = new HpAlmTask(getProject(), new Entity("defect", 85));
        taskManager.activateTask(task, false, false);
    }

    @Test
    public void testTaskManagerEvent_default() {
        testActivate();

        handler.async();
        taskManagerIntegration.addListener(new TaskManagerIntegration.Listener() {
            @Override
            public void taskEntityActivated(final Entity entity) {
                taskManagerIntegration.removeListener(this);
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertNull(entity);
                    }
                });
            }
        });

        activateDefault();
    }

    private void activateDefault() {
        Task task = taskManagerIntegration._getDefaultTask();
        Assert.assertNotNull(task);
        taskManager.activateTask(task, false, false);
    }
}
