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

import com.hp.alm.ali.idea.MultiTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.model.User;
import com.hp.alm.ali.idea.model.parser.UserList;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ProjectUserServiceTest extends MultiTest {

    private ProjectUserService projectUserService;

    @Before
    public void preClean() throws IOException {
        projectUserService = getComponent(ProjectUserService.class);
        projectUserService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testGetUser() {
        RestInvocations.loadProjectUsers(handler);

        User tester = projectUserService.getUser("tester");
        Assert.assertEquals("tester", tester.getUsername());
        Assert.assertEquals("Integration Test", tester.getFullName());

        // served from the cache
        projectUserService.getUser("tester");
    }

    @Test
    public void testGetUser_nonExisting() {
        RestInvocations.loadProjectUsers(handler);

        Assert.assertNull(projectUserService.getUser("nonExisting"));
    }

    @Test
    public void testTryGetUser() {
        User tester = projectUserService.tryGetUser("tester");
        Assert.assertNull(tester);

        RestInvocations.loadProjectUsers(handler);
        projectUserService.getUser("tester");

        tester = projectUserService.tryGetUser("tester");
        Assert.assertEquals("tester", tester.getUsername());
        Assert.assertEquals("Integration Test", tester.getFullName());
    }

    @Test
    public void testGetUserList() {
        RestInvocations.loadProjectUsers(handler);

        UserList userList = projectUserService.getUserList();
        Assert.assertEquals(1, userList.size());
        User tester = userList.getUser("tester");
        Assert.assertEquals("tester", tester.getUsername());
        Assert.assertEquals("Integration Test", tester.getFullName());

        // served from the cache
        projectUserService.getUserList();
    }

    @Test
    public void testLoadUserAsync() throws InterruptedException {
        RestInvocations.loadProjectUsers(handler);

        handler.async();
        projectUserService.loadUserAsync("tester", new NonDispatchTestCallback<User>(handler) {
            @Override
            public void evaluate(User user) {
                Assert.assertEquals("tester", user.getUsername());
                Assert.assertEquals("Integration Test", user.getFullName());
            }
        });
    }


    @Test
    public void testLoadUserAsync_dispatch() throws InterruptedException {
        RestInvocations.loadProjectUsers(handler);

        handler.async();
        projectUserService.loadUserAsync("tester", new DispatchTestCallback<User>(handler) {
            @Override
            public void evaluate(User user) {
                Assert.assertEquals("tester", user.getUsername());
                Assert.assertEquals("Integration Test", user.getFullName());
            }
        });
    }

    @Test
    public void testLoadUsersAsync() throws InterruptedException {
        RestInvocations.loadProjectUsers(handler);

        handler.async();
        projectUserService.loadUsersAsync(new NonDispatchTestCallback<UserList>(handler) {
            @Override
            public void evaluate(UserList userList) {
                Assert.assertEquals(1, userList.size());
                User tester = userList.getUser("tester");
                Assert.assertEquals("tester", tester.getUsername());
                Assert.assertEquals("Integration Test", tester.getFullName());
            }
        });
    }

    @Test
    public void testLoadUsersAsync_dispatch() throws InterruptedException {
        RestInvocations.loadProjectUsers(handler);

        handler.async();
        projectUserService.loadUsersAsync(new DispatchTestCallback<UserList>(handler) {
            @Override
            public void evaluate(UserList userList) {
                Assert.assertEquals(1, userList.size());
                User tester = userList.getUser("tester");
                Assert.assertEquals("tester", tester.getUsername());
                Assert.assertEquals("Integration Test", tester.getFullName());
            }
        });
    }

    @Test
    public void testGetUserFullName() {
        RestInvocations.loadProjectUsers(handler);

        String fullName = projectUserService.getUserFullName("tester");
        Assert.assertEquals("Integration Test", fullName);

        // served from the cache
        projectUserService.getUserFullName("tester");
    }

    @Test
    public void testGetUserFullName_nonexisting() {
        RestInvocations.loadProjectUsers(handler);

        String fullName = projectUserService.getUserFullName("nonexisting_tester");
        Assert.assertNull(fullName);

        // served from the cache
        projectUserService.getUserFullName("nonexisting_tester");
    }
}
