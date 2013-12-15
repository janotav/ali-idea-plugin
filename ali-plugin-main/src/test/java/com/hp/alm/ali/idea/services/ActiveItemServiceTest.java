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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IdeaCompatibility;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.tasks.TasksApi;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.content.detail.EntityDetail;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskListenerAdapter;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.impl.LocalTaskImpl;
import com.intellij.ui.content.ContentManager;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ActiveItemServiceTest extends IntellijTest {

    private ActiveItemService activeItemService;
    private TaskManager taskManager;
    private TasksApi tasksApi;

    public ActiveItemServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() {
       activeItemService = getComponent(ActiveItemService.class);
       taskManager = getComponent(TaskManager.class);
       tasksApi = getComponent(IdeaCompatibility.class).getComponent(TasksApi.class);
    }

    @Test
    public void testGetState() {
        selectDefect();

        Element state = activeItemService.getState();
        Assert.assertEquals("ActiveItemService", state.getName());
        Assert.assertEquals("defect", state.getAttributeValue("type"));
        Assert.assertEquals("1", state.getAttributeValue("id"));
    }

    @Test
    public void testGetState_none() {
        selectNothing();

        Element state = activeItemService.getState();
        Assert.assertEquals("ActiveItemService", state.getName());
        Assert.assertNull(state.getAttributeValue("type"));
        Assert.assertNull(state.getAttributeValue("id"));
    }

    @Test
    public void testLoadState() {
        selectNothing();

        Element state = new Element("ActiveItemService");
        state.setAttribute("type", "defect");
        state.setAttribute("id", "2");
        activeItemService.loadState(state);

        checkActivateItem("defect", 2);
    }

    @Test
    public void testLoadState_none() {
        selectNothing();

        Element state = new Element("ActiveItemService");
        activeItemService.loadState(state);

        Assert.assertNull(activeItemService.getActiveItem());
    }

    @Test
    public void testActivate() {
        selectNothing();

        handler.async();
        activeItemService.addListener(new ActiveItemService.Listener() {
            @Override
            public void onActivated(final EntityRef ref) {
                activeItemService.removeListener(this);
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertNotNull(ref);
                        Assert.assertEquals(2, ref.id);
                        Assert.assertEquals("requirement", ref.type);
                    }
                });
            }
        });

        activeItemService.activate(new Entity("requirement", 2), false, false);
        checkActivateItem("requirement", 2);
    }

    @Test
    public void testActivate_taskManager() {
        LocalTaskImpl localTask = new LocalTaskImpl("test" + System.currentTimeMillis(), "testing");
        tasksApi.activateTask(localTask);

        handler.async();
        taskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskActivated(final LocalTask localTask) {
                taskManager.removeTaskListener(this);
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("Default", localTask.getId());
                    }
                });
            }
        });
        activeItemService.activate(null, true, false);
        Assert.assertNull(activeItemService.getActiveItem());
    }

    @Test
    public void testActivate_select() {
        selectEntityDetailExecutionCheck(new Runnable() {
            @Override
            public void run() {
                activeItemService.activate(new Entity("requirement", 4), false, true);
            }
        });
    }

    @Test
    public void testSelectEntityDetail() {
        selectEntityDetailExecutionCheck(new Runnable() {
            @Override
            public void run() {
                activeItemService.selectEntityDetail(new Entity("requirement", 4));
            }
        });
    }

    private void selectEntityDetailExecutionCheck(Runnable action) {
        RestInvocations.loadMetadata(handler, "requirement");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=cover-status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.feature-id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,last-modified,type-id,release-backlog-item.invested,release-backlog-item.sprint-id,release-backlog-item.watch-id,comments,description,release-backlog-item.story-points,release-backlog-item.team-id,release-backlog-item.entity-id,release-backlog-item.remaining,products-id,owner,release-backlog-item.linked-entities-info,no-of-sons,no-of-blis,id,release-backlog-item.entity-name,name,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.theme-id,req-time,release-backlog-item.product-id,release-backlog-item.blocked,release-backlog-item.kanban-status-id,attachment,release-backlog-item.kan-parent-duration,req-reviewed,req-priority,release-backlog-item.id&query={id[4]}&order-by={}", 200);
        RestInvocations.loadCustomizationEntities(handler, getProject());

        action.run();

        // the content manager mock used in tests doesn't allow to listen for events: wait for the requests
        // moreover because the installed entity detail becomes an active source of events, test has to be isolated

        handler.consume();

        ContentManager contentManager = getComponent(ToolWindowManager.class).getToolWindow(AliContentFactory.TOOL_WINDOW_DETAIL).getContentManager();
        Assert.assertEquals(1, contentManager.getContents().length);
        EntityDetail detail = (EntityDetail)contentManager.getContents()[0].getComponent();
        Assert.assertEquals(4, detail.getEntity().getId());
        Assert.assertEquals("requirement", detail.getEntity().getType());
        contentManager.removeAllContents(true);
        detail._unregister();
    }

    private void checkActivateItem(String type, int id) {
        EntityRef ref = activeItemService.getActiveItem();
        Assert.assertNotNull(ref);
        Assert.assertEquals(type, ref.type);
        Assert.assertEquals(id, ref.id);
    }

    private void selectDefect() {
        activeItemService.activate(new Entity("defect", 1), false, false);
        checkActivateItem("defect", 1);
    }

    private void selectNothing() {
        activeItemService.activate(null, false, false);
        Assert.assertNull(activeItemService.getActiveItem());
    }
}
