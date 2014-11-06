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
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.model.AuthenticationInfo;
import com.hp.alm.ali.idea.model.Entity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

public class ApmUIServiceTest extends IntellijTest {

    public ApmUIServiceTest() {
        super(ServerVersion.AGM);
    }

    private ApmUIService apmUIService;

    @Before
    public void preClean() {
        apmUIService = getComponent(ApmUIService.class);
    }

    @Test
    public void testCreateDefectInRelease() {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/apmuiservices/additemservice/createdefectinrelease", 200)
                .expectBody("description=something&detectedBy=me&detectedOn=&featureID=1439&name=somewhat&releaseId=1001&sevirity=2-High&sprintID=1001&teamID=101&productGroupId=1200")
                .content("apmUIServiceTest_defect.xml");
        RestInvocations.loadMetadata(handler, "project-task");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/project-tasks?fields=&query={release-backlog-item-id[4289]}&order-by={}", 200)
                .content("apmUIServiceTest_task.xml");

        final LinkedList<EntityCheck> list = new LinkedList<EntityCheck>();
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("release-backlog-item", entity.getType());
                Assert.assertEquals(4289, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
            }
        });
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("defect", entity.getType());
                Assert.assertEquals(86, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
                Assert.assertEquals("4289", entity.getPropertyValue("release-backlog-item.id"));
            }
        });
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("project-task", entity.getType());
                Assert.assertEquals(107, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
            }
        });
        addEntityListener(new EntityAdapter() {
            @Override
            public void entityLoaded(Entity entity, Event event) {
                handler.done(list.removeFirst().set(entity, event));
            }
        });

        handler.async(3);
        apmUIService.createDefectInRelease("something", "somewhat", "2-High", "me", 1001, 1001, 101, 1439, 1200);
    }

    @Test
    public void testCreateRequirementInRelease() {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/apmuiservices/additemservice/createrequirementinrelease", 200)
                .expectBody("description=something&featureID=1439&name=somewhat&parentID=0&priority=2-High&releaseId=1001&reqType=70&sprintID=1001&storyPoints=1&teamID=101&productGroupId=1200")
                .content("apmUIServiceTest_requirement.xml");
        RestInvocations.loadMetadata(handler, "project-task");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/project-tasks?fields=&query={release-backlog-item-id[4289]}&order-by={}", 200)
                .content("apmUIServiceTest_task.xml");

        final LinkedList<EntityCheck> list = new LinkedList<EntityCheck>();
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("release-backlog-item", entity.getType());
                Assert.assertEquals(4289, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
            }
        });
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("requirement", entity.getType());
                Assert.assertEquals(4199, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
                Assert.assertEquals("4289", entity.getPropertyValue("release-backlog-item.id"));
            }
        });
        list.add(new EntityCheck() {
            @Override
            public void run() {
                Assert.assertEquals("project-task", entity.getType());
                Assert.assertEquals(107, entity.getId());
                Assert.assertEquals(EntityListener.Event.CREATE, event);
            }
        });
        addEntityListener(new EntityAdapter() {
            @Override
            public void entityLoaded(Entity entity, Event event) {
                handler.done(list.removeFirst().set(entity, event));
            }
        });

        handler.async(3);
        apmUIService.createRequirementInRelease("something", "somewhat", "2-High", 1, 1001, 1001, 101, 1439, 1200);
    }

    @Test
    public void testCreationFailure() {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/apmuiservices/additemservice/createdefectinrelease", 500)
                .responseBody("Bad day");
        Entity defect = apmUIService.createDefectInRelease("something", "somewhat", "2-High", "me", 1001, 1001, 101, 1439, 1200);
        Assert.assertNull(defect);
        checkError("Bad day");
    }

    @Test
    public void testGetAuthenticationInfo() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/apmuiservices/configurationusers/authentication-info", 200)
                .content("apmUIServiceTest_authenticationInfo.json");
        AuthenticationInfo authenticationInfo = apmUIService.getAuthenticationInfo();
        Assert.assertEquals(1, authenticationInfo.getAssignedWorkspaces().size());
        Assert.assertEquals(1000, (int) authenticationInfo.getAssignedWorkspaces().iterator().next());
    }

    private abstract static class EntityCheck implements Runnable {

        Entity entity;
        EntityListener.Event event;

        private EntityCheck set(Entity entity, EntityListener.Event event) {
            this.entity = entity;
            this.event = event;
            return this;
        }
    }
}
