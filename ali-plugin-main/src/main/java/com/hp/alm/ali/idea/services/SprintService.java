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

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.CachingEntityListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.ObjectUtils;
import org.jdom.Element;

import javax.swing.SortOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@State(
        name = "SprintService",
        storages = { @Storage(id = "default",file = "$WORKSPACE_FILE$") }
)
public class SprintService implements PersistentStateComponent<Element>, ServerTypeListener, CachingEntityListener {

    private static SimpleDateFormat ALM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private EntityService entityService;
    private RestService restService;

    final private Selector releaseSelector = new Selector();
    final private Selector sprintSelector = new Selector();
    final private Selector teamSelector = new Selector();

    private WeakListeners<Listener> listeners = new WeakListeners<Listener>();

    public SprintService(Project project) {
        entityService = project.getComponent(EntityService.class);
        restService = project.getComponent(RestService.class);

        entityService.addEntityListener(this);
        restService.addServerTypeListener(this);
    }

    public Element getState() {
        Element element = new Element(getClass().getSimpleName());
        addElement(element, releaseSelector.selected);
        addElement(element, sprintSelector.selected);
        addElement(element, teamSelector.selected);
        return element;
    }

    private void addElement(Element element, Entity entity) {
        if(entity != null) {
            element.setAttribute(entity.getType()+"-id", String.valueOf(entity.getId()));
            element.setAttribute(entity.getType()+"-name", entity.getPropertyValue("name"));
        }
    }

    public void loadState(Element state) {
        selectSprint(loadEntity(state, "release-cycle"));
        selectTeam(loadEntity(state, "team"));
        selectRelease(loadEntity(state, "release"));
    }

    private Entity loadEntity(Element element, String type) {
        String id = element.getAttributeValue(type + "-id");
        if(id != null) {
            Entity entity = new Entity(type, Integer.valueOf(id));
            entity.setProperty("name", element.getAttributeValue(type+"-name"));
            return entity;

        } else {
            return null;
        }
    }

    // method should be considered private except for usage in tests
    synchronized void resetValues() {
        teamSelector.values = null;
        sprintSelector.values = null;
        releaseSelector.values = null;
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(ServerType.AGM.equals(restService.getServerTypeIfAvailable())) {
            synchronized (this) {
                resetValues();
                final Entity release = this.releaseSelector.selected;
                if(release != null) {
                    teamSelector.requestRunning = true;
                    sprintSelector.requestRunning = true;
                    releaseSelector.requestRunning = true;
                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            loadTeams(release);
                            loadSprints(release);
                            loadReleases();
                        }
                    });
                } else {
                    this.releaseSelector.requestRunning = true;
                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            loadReleases();
                        }
                    });
                }
            }
        }
    }

    public synchronized Entity getRelease() {
        return releaseSelector.selected;
    }

    public synchronized Entity getSprint() {
        return sprintSelector.selected;
    }

    public synchronized Entity getTeam() {
        return teamSelector.selected;
    }

    private void loadReleases() {
        EntityQuery query = new EntityQuery("release");
        query.addColumn("id", 1);
        query.addColumn("name", 1);
        query.addColumn("start-date", 1);
        query.addColumn("end-date", 1);
        EntityList list = EntityList.empty();
        try {
            list = entityService.query(query);
        } finally {
            synchronized (this) {
                releaseSelector.values = list;
                releaseSelector.requestRunning = false;

                if(!releaseSelector.values.contains(releaseSelector.selected)) {
                    selectRelease(findClosest(list));
                }
                notifyAll();
            }
        }
    }

    private void loadSprints(final Entity release) {
        EntityQuery query = new EntityQuery("release-cycle");
        query.addColumn("id", 1);
        query.addColumn("name", 1);
        query.addColumn("tense", 1);
        query.addColumn("start-date", 1);
        query.addColumn("end-date", 1);
        query.setValue("parent-id", String.valueOf(release.getId()));
        query.addOrder("start-date", SortOrder.ASCENDING);
        EntityList list = EntityList.empty();
        try {
            list = entityService.query(query);
        } finally {
            synchronized (this) {
                sprintSelector.values = list;
                sprintSelector.requestRunning = false;

                if(!sprintSelector.values.contains(sprintSelector.selected)) {
                    selectSprint(findClosest(list));
                }
                notifyAll();
            }
        }
    }

    private Entity findClosest(List<Entity> list) {
        if(!list.isEmpty()) {
            final long now = System.currentTimeMillis();
            // sort according to time distance
            Collections.sort(list, new Comparator<Entity>() {
                @Override
                public int compare(Entity entity1, Entity entity2) {
                    return (int)(distance(now, entity1) - distance(now, entity2));
                }
            });
            return list.get(0);
        } else {
            return null;
        }
    }

    private void loadTeams(final Entity release) {
        EntityQuery query = new EntityQuery("team");
        query.addColumn("id", 1);
        query.addColumn("name", 1);
        query.setValue("release-id", release.getPropertyValue("id"));
        query.addOrder("name", SortOrder.ASCENDING);
        EntityList list = EntityList.empty();
        try {
            list = entityService.query(query);
        } finally {
            synchronized (this) {
                teamSelector.values = list;
                teamSelector.requestRunning = false;

                if(!teamSelector.values.contains(teamSelector.selected)) {
                    // TODO: choose my team
                    selectTeam(list.isEmpty()? null: list.get(0));
                }
                notifyAll();
            }
        }
    }

    private void fireSprintSelected() {
        listeners.fire(new WeakListeners.Action<Listener>() {
            public void fire(Listener listener) {
                listener.onSprintSelected(sprintSelector.selected);
            }
        });
    }

    private void fireTeamSelected() {
        listeners.fire(new WeakListeners.Action<Listener>() {
            public void fire(Listener listener) {
                listener.onTeamSelected(teamSelector.selected);
            }
        });
    }

    private void fireReleaseSelected() {
        listeners.fire(new WeakListeners.Action<Listener>() {
            public void fire(Listener listener) {
                listener.onReleaseSelected(releaseSelector.selected);
            }
        });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized void selectRelease(final Entity release) {
        if((release != null && release.equals(this.releaseSelector.selected)) ||
                (release == null && this.releaseSelector.selected == null)) {
            return;
        }

        this.releaseSelector.selected = release;
        if(release != null) {
            if(ServerType.AGM.equals(restService.getServerTypeIfAvailable())) {
                sprintSelector.requestRunning = true;
                teamSelector.requestRunning = true;
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        loadSprints(release);
                        loadTeams(release);
                    }
                });
            }
        } else {
            selectSprint(null);
            selectTeam(null);
        }
        fireReleaseSelected();
    }

    public synchronized void selectSprint(Entity sprint) {
        // if sprint has different tense, we want the event fired (otherwise the inactive sprint warning would not be
        // displayed when the original sprint was loaded from persisted configuration)
        if((sprint != null && sprint.equals(this.sprintSelector.selected) && isSameTense(sprint, this.sprintSelector.selected)) ||
                (sprint == null && this.sprintSelector.selected == null)) {
            return;
        }

        this.sprintSelector.selected = sprint;
        fireSprintSelected();
    }

    public synchronized void selectTeam(Entity team) {
        if((team != null && team.equals(this.teamSelector.selected)) ||
                (team == null && this.teamSelector.selected == null)) {
            return;
        }

        this.teamSelector.selected = team;
        fireTeamSelected();
    }

    public synchronized Entity getCurrentSprint() {
        if(sprintSelector.values == null) {
            return null;
        }

        for(Entity sprint: sprintSelector.values) {
            if(isCurrentSprint(sprint)) {
                return sprint;
            }
        }

        return  null;
    }

    private synchronized EntityList getValues(final Selector selector, final Runnable loader) {
        while(true) {
            if(selector.requestRunning) {
                // request running
            } else if(selector.values != null) {
                return selector.values;
            } else {
                selector.requestRunning = true;
                ApplicationManager.getApplication().executeOnPooledThread(loader);
            }
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized EntityList getTeams() {
        final Entity release = this.releaseSelector.selected;
        if(release != null) {
            return getValues(teamSelector, new Runnable() {
                @Override
                public void run() {
                    loadTeams(release);
                }
            });
        } else {
            return null;
        }
    }

    public EntityList getSprints() {
        final Entity release = this.releaseSelector.selected;
        if(release != null) {
            return getValues(sprintSelector, new Runnable() {
                @Override
                public void run() {
                    loadSprints(release);
                }
            });
        } else {
            return null;
        }
    }

    public EntityList getReleases() {
        return getValues(releaseSelector, new Runnable() {
            @Override
            public void run() {
                loadReleases();
            }
        });
    }

    public static boolean isSameTense(Entity sprint1, Entity sprint2) {
        return ObjectUtils.equals(sprint1.getProperty("tense"), sprint2.getProperty("tense"));
    }

    public static boolean isCurrentSprint(Entity sprint) {
        return "CURRENT".equalsIgnoreCase(sprint.getPropertyValue("tense"));
    }

    public static long distance(long now, Entity entity) {
        try {
            Date startDate = ALM_DATE_FORMAT.parse(entity.getPropertyValue("start-date"));
            if(now < startDate.getTime()) {
                // future release
                return startDate.getTime() - now;
            }

            Date endDate = ALM_DATE_FORMAT.parse(entity.getPropertyValue("end-date"));
            // make end-date inclusive
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.add(Calendar.DATE, 1);
            endDate = calendar.getTime();

            if(now > endDate.getTime()) {
                return now - endDate.getTime();
            }

            return 0;
        } catch(Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public Entity lookup(final EntityRef ref) {
        if("release".equals(ref.type)) {
            return find(releaseSelector, ref);
        } else if("release-cycle".equals(ref.type)) {
            return find(sprintSelector, ref);
        } else if("team".equals(ref.type)) {
            return find(teamSelector, ref);
        } else {
            return null;
        }
    }

    private synchronized Entity find(Selector selector, EntityRef ref) {
        if(selector.values != null) {
            int i = selector.values.indexOf(new Entity(ref.type, ref.id));
            if(i >= 0) {
                return selector.values.get(i);
            }
        }
        return null;
    }

    @Override
    public void entityLoaded(Entity entity, Event event) {
        // no need to implement as long as nobody modifies these entity types
    }

    @Override
    public void entityNotFound(EntityRef ref, boolean removed) {
    }

    public static interface Listener {

        void onReleaseSelected(Entity release);

        void onSprintSelected(Entity sprint);

        void onTeamSelected(Entity team);

    }

    private static class Selector {
        private boolean requestRunning;
        private Entity selected;
        private EntityList values;
    }
}
