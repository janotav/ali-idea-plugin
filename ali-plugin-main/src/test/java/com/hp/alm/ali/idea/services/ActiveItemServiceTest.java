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
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.tasks.TasksApi;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.util.ApplicationUtil;
import com.hp.alm.ali.idea.util.DetailUtil;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskListenerAdapter;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.impl.LocalTaskImpl;
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
       tasksApi = getComponent(TasksApi.class);
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

        ApplicationUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Test
    public void testActivate_select() {
        handler.async();

        getComponent(DetailUtil.class)._setLauncher(new DetailUtil.Launcher() {
            @Override
            public void loadDetail(Project project, final Entity entity, final boolean show, final boolean select) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("requirement", entity.getType());
                        Assert.assertEquals(4, entity.getId());
                        Assert.assertEquals(true, show);
                        Assert.assertEquals(true, select);
                    }
                });
            }
        });

        activeItemService.activate(new Entity("requirement", 4), false, true);
    }

    @Test
    public void testSelectEntityDetail() throws Throwable {
        handler.async();

        getComponent(DetailUtil.class)._setLauncher(new DetailUtil.Launcher() {
            @Override
            public void loadDetail(Project project, final Entity entity, final boolean show, final boolean select) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("requirement", entity.getType());
                        Assert.assertEquals(4, entity.getId());
                        Assert.assertEquals(true, show);
                        Assert.assertEquals(true, select);
                    }
                });
            }
        });

        activeItemService.selectEntityDetail(new Entity("requirement", 4));
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
