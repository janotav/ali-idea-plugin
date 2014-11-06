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

import com.hp.alm.ali.Handler;
import com.hp.alm.ali.Isolated;
import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.intellij.openapi.application.ApplicationManager;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Isolated // Current release manipulation introduces various side-effects, better avoid the headache.
public class SprintServiceTest extends IntellijTest {

    private SprintService sprintService;
    private MetadataService metadataService;
    private MetadataSimpleService metadataSimpleService;

    public SprintServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        sprintService = getComponent(SprintService.class);

        metadataService = getComponent(MetadataService.class);
        metadataSimpleService = getComponent(MetadataSimpleService.class);
    }

    @Test
    public void testGetState() throws Throwable {
        selectTestRelease();

        Element state = sprintService.getState();
        Assert.assertEquals("SprintService", state.getName());
        Assert.assertEquals("1001", state.getAttributeValue("release-id"));
        Assert.assertEquals("Test Release", state.getAttributeValue("release-name"));
        Assert.assertEquals("1002", state.getAttributeValue("release-cycle-id"));
        Assert.assertEquals("Sprint 2", state.getAttributeValue("release-cycle-name"));
        Assert.assertEquals("101", state.getAttributeValue("team-id"));
        Assert.assertEquals("The Team", state.getAttributeValue("team-name"));
    }

    @Test
    public void testGetState_None() throws Throwable {
        Element state = sprintService.getState();
        Assert.assertEquals("SprintService", state.getName());
        Assert.assertNull(state.getAttributeValue("release-id"));
        Assert.assertNull(state.getAttributeValue("release-name"));
        Assert.assertNull(state.getAttributeValue("release-cycle-id"));
        Assert.assertNull(state.getAttributeValue("release-cycle-name"));
        Assert.assertNull(state.getAttributeValue("team-id"));
        Assert.assertNull(state.getAttributeValue("team-name"));
    }

    @Test
    public void testLoadState() throws Throwable {
        sprintTeamRequests_TestRelease(handler);

        WaitForEventsListener listener = new WaitForEventsListener(sprintService);

        Element state = new Element("SprintService");
        state.setAttribute("release-id", "1001");
        state.setAttribute("release-name", "Test Release");
        state.setAttribute("release-cycle-id", "1001");
        state.setAttribute("release-cycle-name", "Sprint 1");
        state.setAttribute("team-id", "101");
        state.setAttribute("team-name", "The Team");
        sprintService.loadState(state);

        handler.consume();

        listener.waitForEvents(1001, 1001, 101);

        Assert.assertEquals(1001, sprintService.getRelease().getId());
        Assert.assertEquals(1001, sprintService.getSprint().getId());
        Assert.assertEquals(101, sprintService.getTeam().getId());
    }

    @Test
    public void testLoadState_None() throws Throwable {
        selectTestRelease();

        WaitForEventsListener listener = new WaitForEventsListener(sprintService);

        Element state = new Element("SprintService");
        sprintService.loadState(state);

        listener.waitForEvents(null, null, null);

        Assert.assertNull(sprintService.getRelease());
        Assert.assertNull(sprintService.getSprint());
        Assert.assertNull(sprintService.getTeam());
    }

    @Test
    public void testConnectedTo() throws Throwable {
        selectTestRelease();

        RestInvocations.getAuthenticationInfo(handler);
        releaseSprintTeamRequests_TestRelease(handler);

        // re-connecting triggers data reload
        getComponent(RestService.class).setServerType(ServerType.AGM);

        handler.consume();
    }

    @Test
    public void testSelectRelease() throws Throwable {
        WaitForEventsListener listener = new WaitForEventsListener(sprintService);
        sprintTeamRequests_TestRelease(handler);

        // sprint and team are retrieved and selected
        sprintService.selectRelease(new Entity("release", 1001));

        listener.waitForEvents(1001, 1002, 101);
    }

    @Test
    public void testSelectRelease_None() throws Throwable {
        selectTestRelease();

        WaitForEventsListener listener = new WaitForEventsListener(sprintService);

        // sprint and team are cleared too
        sprintService.selectRelease(null);

        listener.waitForEvents(null, null, null);
    }

    @Test
    public void testSelectSprint() throws Throwable {
        selectTestRelease();

        WaitForEventsListener listener = new WaitForEventsListener(sprintService, false, true, false);

        sprintService.selectSprint(new Entity("release-cycle", 1001));

        listener.waitForEvents(null, 1001, null);
    }

    @Test
    public void testSelectTeam() throws Throwable {
        selectTestRelease();

        WaitForEventsListener listener = new WaitForEventsListener(sprintService, false, false, true);

        sprintService.selectTeam(new Entity("team", 102));

        listener.waitForEvents(null, null, 102);
    }

    @Test
    public void testGetReleases() throws Throwable {
        sprintService.resetValues();

        releaseSprintTeamRequests_TestRelease(handler);

        for(int i = 0; i < 3; i++) {
            handler.async();
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    final EntityList releases = sprintService.getReleases();
                    handler.done(new Runnable() {
                        @Override
                        public void run() {
                            Assert.assertEquals(2, releases.size());
                            Assert.assertEquals(1001, releases.get(0).getId());
                            Assert.assertEquals(1000, releases.get(1).getId());
                        }
                    });
                }
            });
        }

        handler.consume();
    }

    @Test
    public void testGetSprints() throws Throwable {
        selectTestRelease();

        sprintService.resetValues();

        sprintRequests_TestRelease(handler);

        for(int i = 0; i < 3; i++) {
            handler.async();
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    final EntityList sprints = sprintService.getSprints();
                    handler.done(new Runnable() {
                        @Override
                        public void run() {
                            Assert.assertEquals(2, sprints.size());
                            Assert.assertEquals(1001, sprints.get(0).getId());
                            Assert.assertEquals(1002, sprints.get(1).getId());
                        }
                    });
                }
            });
        }

        handler.consume();
    }

    @Test
    public void testGetTeams() throws Throwable {
        selectTestRelease();

        sprintService.resetValues();

        teamRequests_TestRelease(handler);

        for(int i = 0; i < 3; i++) {
            handler.async();
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    final EntityList teams = sprintService.getTeams();
                    handler.done(new Runnable() {
                        @Override
                        public void run() {
                            Assert.assertEquals(2, teams.size());
                            Assert.assertEquals(101, teams.get(0).getId());
                            Assert.assertEquals(102, teams.get(1).getId());
                        }
                    });
                }
            });
        }

        handler.consume();
    }

    @Test
    public void testGetRelease() throws Throwable {
        selectTestRelease();

        Assert.assertEquals(1001, sprintService.getRelease().getId());
    }

    @Test
    public void testGetSprint() throws Throwable {
        selectTestRelease();

        Assert.assertEquals(1002, sprintService.getSprint().getId());
    }

    @Test
    public void testGetTeam() throws Throwable {
        selectTestRelease();

        Assert.assertEquals(101, sprintService.getTeam().getId());
    }

    @Test
    public void testGetCurrentSprint() throws Throwable {
        selectTestRelease();

        Assert.assertEquals(1002, sprintService.getCurrentSprint().getId());
    }

    @Test
    public void testGetCurrentSprint_notLoaded() throws Throwable {
        sprintService.resetValues();

        Assert.assertNull(sprintService.getCurrentSprint());
    }

    @Test
    public void testGetCurrentSprint_notMatching() throws Throwable {
        Assert.assertNull(sprintService.getCurrentSprint());
    }

    @Test
    public void testIsCurrentSprint() throws Throwable {
        Entity sprint = new Entity("release-cycle", 1);
        sprint.setProperty("tense", "PAST");
        Assert.assertFalse(SprintService.isCurrentSprint(sprint));
        sprint.setProperty("tense", "CURRENT");
        Assert.assertTrue(SprintService.isCurrentSprint(sprint));
        sprint.setProperty("tense", "FUTURE");
        Assert.assertFalse(SprintService.isCurrentSprint(sprint));
    }

    @Test
    public void testDistance() throws Throwable {
        Entity sprint = new Entity("release-cycle", 1);

        // invalid value
        Assert.assertEquals(Long.MAX_VALUE, SprintService.distance(new Date().getTime(), sprint));

        sprint.setProperty("start-date", "2013-05-02");
        sprint.setProperty("end-date", "2013-05-09");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // future sprint
        Assert.assertEquals(1000, SprintService.distance(format.parse("2013-05-01 23:59:59").getTime(), sprint));

        // past sprint
        Assert.assertEquals(1000, SprintService.distance(format.parse("2013-05-10 00:00:01").getTime(), sprint));

        // current sprint
        Assert.assertEquals(0, SprintService.distance(format.parse("2013-05-07 13:12:11").getTime(), sprint));

    }

    @Test
    public void testLookup() throws Throwable {
        selectTestRelease();

        Assert.assertEquals("Test Release", sprintService.lookup(new EntityRef("release", 1001)).getPropertyValue("name"));
        Assert.assertEquals("Sprint 2", sprintService.lookup(new EntityRef("release-cycle", 1002)).getPropertyValue("name"));
        Assert.assertEquals("The Others", sprintService.lookup(new EntityRef("team", 102)).getPropertyValue("name"));
        Assert.assertNull(sprintService.lookup(new EntityRef("defect", 1)));

        sprintService.resetValues();

        Assert.assertNull(sprintService.lookup(new EntityRef("release", 1001)));
        Assert.assertNull(sprintService.lookup(new EntityRef("release-cycle", 1002)));
        Assert.assertNull(sprintService.lookup(new EntityRef("team", 102)));
        Assert.assertNull(sprintService.lookup(new EntityRef("defect", 1)));
    }

    @Test
    public void testRespondsToCacheRequests() throws Throwable {
        selectTestRelease();

        handler.async();
        getComponent(EntityService.class).requestCachedEntity(new EntityRef("release-cycle", 1002), Arrays.asList("name"), new EntityListener() {
            @Override
            public void entityLoaded(final Entity entity, Event event) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("Sprint 2", entity.getPropertyValue("name"));
                    }
                });
            }

            @Override
            public void entityNotFound(EntityRef ref, boolean removed) {
                handler.fail("not found in cache");
            }
        });
    }

    private void releaseSprintTeamRequests_TestRelease(Handler handler) {
        sprintTeamRequests_TestRelease(handler);
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/releases?fields=id,name,start-date,end-date,product-group-id&query={product-group-id[1000]}&order-by={}", 200)
                .content("sprintServiceTest_releases.xml");
    }

    private void sprintTeamRequests_TestRelease(Handler handler) {
        sprintRequests_TestRelease(handler);
        teamRequests_TestRelease(handler);
    }

    private void sprintRequests_TestRelease(Handler handler) {
        RestInvocations.loadMetadata(handler, "release-cycle");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/release-cycles?fields=id,name,tense,start-date,end-date&query={parent-id[1001]}&order-by={start-date[ASC]}", 200)
                .content("sprintServiceTest_sprints.xml");
    }

    private void teamRequests_TestRelease(Handler handler) {
        RestInvocations.loadMetadata(handler, "team");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/teams?fields=id,name&query={release-id[1001]}&order-by={name[ASC]}", 200)
                .content("sprintServiceTest_teams.xml");
    }

    private void releaseSprintTeamRequests_None(Handler handler, boolean all) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/releases?fields=id,name,start-date,end-date&query={}&order-by={}", 200)
                .content("no_entities.xml");
        if(all) {
            RestInvocations.loadMetadata(handler, "team");
            RestInvocations.loadMetadata(handler, "release-cycle");
            handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/release-cycles?fields=id,name,tense,start-date,end-date&query={parent-id[1001]}&order-by={start-date[ASC]}", 200)
                    .content("no_entities.xml");
            handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/teams?fields=id,name&query={release-id[1001]}&order-by={name[ASC]}", 200)
                    .content("no_entities.xml");
        }
    }

    private void selectTestRelease() throws Throwable {
        if(sprintService.getRelease() != null) {
            return;
        }

        releaseSprintTeamRequests_TestRelease(handler);
        WaitForEventsListener listener = new WaitForEventsListener(sprintService);
        sprintService.connectedTo(ServerType.AGM);
        handler.consume();
        listener.waitForEvents(1001, 1002, 101);
        metadataService.connectedTo(ServerType.NONE);
        metadataSimpleService.connectedTo(ServerType.NONE);
        Assert.assertEquals("1001", sprintService.getState().getAttributeValue("release-id"));
    }

    public static class WaitForEventsListener implements SprintService.Listener {

        private Semaphore semaphore;
        private Entity team;
        private Entity release;
        private Entity sprint;
        private SprintService sprintService;
        private boolean releaseEvent;
        private boolean sprintEvent;
        private boolean teamEvent;
        private StringBuffer fail;

        public WaitForEventsListener(SprintService sprintService) {
            this(sprintService, true, true, true);
        }

        public WaitForEventsListener(SprintService sprintService, boolean releaseEvent, boolean sprintEvent, boolean teamEvent) {
            this.semaphore = new Semaphore(0);
            this.sprintService = sprintService;
            this.releaseEvent = initSemaphore(releaseEvent);
            this.sprintEvent = initSemaphore(sprintEvent);
            this.teamEvent = initSemaphore(teamEvent);
            this.fail = new StringBuffer();
            sprintService.addListener(this);
        }

        private boolean initSemaphore(boolean enabled) {
            if(!enabled) {
                semaphore.release(1);
            }
            return enabled;
        }

        @Override
        public void onReleaseSelected(Entity release) {
            if(!releaseEvent) {
                fail.append("unexpected onRelease event: " + release + "; ");
            } else {
                releaseEvent = false;
                this.release = release;
                semaphore.release(1);
            }
        }

        @Override
        public void onSprintSelected(Entity sprint) {
            if(!sprintEvent) {
                fail.append("unexpected onSprint event: " + sprint + "; ");
            } else {
                sprintEvent = false;
                this.sprint = sprint;
                semaphore.release(1);
            }
        }

        @Override
        public void onTeamSelected(Entity team) {
            if(!teamEvent) {
                fail.append("unexpected onTeam event: " + team + "; ");
            } else {
                teamEvent = false;
                this.team = team;
                semaphore.release(1);
            }
        }

        public void waitForEvents(Integer releaseId, Integer sprintId, Integer teamId) throws InterruptedException {
            Assert.assertTrue(semaphore.tryAcquire(3, 2000000, TimeUnit.MILLISECONDS));
            sprintService.removeListener(this);
            if(fail.length() > 0) {
                Assert.fail(fail.toString());
            }
            compare(release, releaseId);
            compare(sprint, sprintId);
            compare(team, teamId);
        }

        private void compare(Entity entity, Integer id) {
            if(id == null) {
                Assert.assertNull(entity);
            } else {
                Assert.assertEquals(id.intValue(), entity.getId());
            }
        }
    }
}
