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
import com.hp.alm.ali.idea.cfg.WorkspaceConfiguration;
import com.hp.alm.ali.idea.rest.OneTimeServerTypeListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class WorkspaceServiceTest extends IntellijTest {

    private WorkspaceConfiguration workspaceConfiguration;
    private WorkspaceService workspaceService;
    private RestService restService;

    public WorkspaceServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        workspaceService = getComponent(WorkspaceService.class);
        workspaceService.connectedTo(ServerType.NONE);

        restService = getComponent(RestService.class);
        workspaceConfiguration = getComponent(WorkspaceConfiguration.class);
    }

    @Test
    public void testSelectWorkspace() throws Throwable {
        RestInvocations.sprintService_getReleases(handler, 1005);

        handler.async();
        restService.addServerTypeListener(new OneTimeServerTypeListener(handler, restService) {
            @Override
            protected void connectedToEvent(ServerType serverType) {
                Assert.assertEquals(ServerType.AGM, serverType);
            }
        });

        try {
            workspaceService.selectWorkspace(1005, "Red");
            handler.consume();

            Assert.assertEquals(1005, (int) workspaceConfiguration.getWorkspaceId());
            Assert.assertEquals(workspaceConfiguration.getWorkspaceName(), "Red");
        } finally {
            workspaceConfiguration.setWorkspaceId(1000);
        }
    }

    @Test
    public void testListWorkspaces() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/product-groups?fields=&query={}&order-by={}", 200)
                .content("workspaceServiceTest_workspaces.xml");

        Map<Integer, String> map = workspaceService.listWorkspaces();
        Assert.assertEquals(3, map.size());
        Assert.assertEquals("All", map.get(-1));
        Assert.assertEquals("Blue", map.get(1000));
        Assert.assertEquals("Green", map.get(1001));
    }

    @Test
    public void testListWorkspaces_caches() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/product-groups?fields=&query={}&order-by={}", 200)
                .content("workspaceServiceTest_workspaces.xml");

        workspaceService.listWorkspaces();

        Map<Integer, String> map = workspaceService.listWorkspaces();
        Assert.assertEquals(3, map.size());
        Assert.assertEquals("All", map.get(-1));
        Assert.assertEquals("Blue", map.get(1000));
        Assert.assertEquals("Green", map.get(1001));
    }

    @Test
    public void testListWorkspacesAsync() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/product-groups?fields=&query={}&order-by={}", 200)
                .content("workspaceServiceTest_workspaces.xml");

        handler.async();
        workspaceService.listWorkspacesAsync(new AbstractCachingService.Callback<Map<Integer, String>>() {
            @Override
            public void loaded(final Map<Integer, String> map) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(3, map.size());
                        Assert.assertEquals("All", map.get(-1));
                        Assert.assertEquals("Blue", map.get(1000));
                        Assert.assertEquals("Green", map.get(1001));
                    }
                });
            }
        });
    }

    @Test
    public void testListWorkspacesAsync_caches() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/product-groups?fields=&query={}&order-by={}", 200)
                .content("workspaceServiceTest_workspaces.xml");

        workspaceService.listWorkspaces();

        handler.async();
        workspaceService.listWorkspacesAsync(new AbstractCachingService.Callback<Map<Integer, String>>() {
            @Override
            public void loaded(final Map<Integer, String> map) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(3, map.size());
                        Assert.assertEquals("All", map.get(-1));
                        Assert.assertEquals("Blue", map.get(1000));
                        Assert.assertEquals("Green", map.get(1001));
                    }
                });
            }
        });
    }
}
