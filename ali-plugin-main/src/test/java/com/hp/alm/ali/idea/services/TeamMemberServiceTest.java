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
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TeamMemberServiceTest extends IntellijTest {

    private TeamMemberService teamMemberService;

    public TeamMemberServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        teamMemberService = getComponent(TeamMemberService.class);
        teamMemberService.connectedTo(ServerType.NONE);

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/team-members?fields=id,name,full-name&query={team-id[101]}&order-by={name[ASC]}", 200)
                .content("teamMemberServiceTest_teamMembers.xml");
    }

    private void checkMembers(List<Entity> list) {
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Jack Koder", list.get(0).getPropertyValue("full-name"));
        Assert.assertEquals("John Stable", list.get(1).getPropertyValue("full-name"));
    }

    @Test
    public void testGetTeamMembers() {
        EntityList list = teamMemberService.getTeamMembers(new Entity("team", 101));
        checkMembers(list);

        // from cache
        teamMemberService.getTeamMembers(new Entity("team", 101));
    }

    @Test
    public void testGetTeamMembersAsync() {
        handler.async();
        teamMemberService.getTeamMembersAsync(new Entity("team", 101), new NonDispatchTestCallback<EntityList>(handler) {
            @Override
            public void evaluate(EntityList list) {
                checkMembers(list);
            }
        });
    }

    @Test
    public void testGetTeamMembersAsync_dispatch() {
        handler.async();
        teamMemberService.getTeamMembersAsync(new Entity("team", 101), new DispatchTestCallback<EntityList>(handler) {
            @Override
            public void evaluate(EntityList list) {
                checkMembers(list);
            }
        });
    }
}
