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

import java.util.Arrays;
import java.util.List;

public class TeamServiceTest extends IntellijTest {

    private TeamService teamService;

    public TeamServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        teamService = getComponent(TeamService.class);
        teamService.connectedTo(ServerType.NONE);

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/teams?fields=id,release-id,name&query={name['The Team']}&order-by={}", 200)
                .content("teamServiceTest_teams.xml");
    }

    @Test
    public void testGetTeam() {
        Entity team = teamService.getTeam("The Team", 1001);
        Assert.assertEquals(101, team.getId());

        // from cache
        teamService.getTeam("The Team", 1001);
    }

    @Test
    public void testGetTeams() {
        List<Entity> list = teamService.getTeams("The Team");
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(101, list.get(0).getId());
        Assert.assertEquals("1000", list.get(0).getPropertyValue("release-id"));
        Assert.assertEquals(101, list.get(1).getId());
        Assert.assertEquals("1001", list.get(1).getPropertyValue("release-id"));

        // from cache
        teamService.getTeams("The Team");
    }

    @Test
    public void testGetMultipleTeams() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/teams?fields=id,release-id,name&query={name['The Others']}&order-by={}", 200)
                .content("teamServiceTest_teams2.xml");

        EntityList list = teamService.getMultipleTeams(Arrays.asList("The Team", "The Others"));
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(101, list.get(0).getId());
        Assert.assertEquals("1000", list.get(0).getPropertyValue("release-id"));
        Assert.assertEquals(101, list.get(1).getId());
        Assert.assertEquals("1001", list.get(1).getPropertyValue("release-id"));
        Assert.assertEquals(102, list.get(2).getId());
        Assert.assertEquals("1001", list.get(2).getPropertyValue("release-id"));

        // from cache
        teamService.getMultipleTeams(Arrays.asList("The Team", "The Others"));
    }
}
