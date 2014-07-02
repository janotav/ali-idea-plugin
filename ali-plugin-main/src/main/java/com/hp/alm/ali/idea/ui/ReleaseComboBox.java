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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

public class ReleaseComboBox extends LazyComboBox implements SprintService.Listener {

    private SprintService sprintService;

    public ReleaseComboBox(Project project) {
        super(project, "release");

        sprintService = project.getComponent(SprintService.class);
        sprintService.addListener(this);

        Entity release = sprintService.getRelease();
        if(release != null) {
            addItem(new ComboItem(release, release.getPropertyValue("name")));
        }

        addItemListener(new NonLoadingItemListener() {
            @Override
            public void doItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object selected = ((ComboItem) e.getItem()).getKey();
                    if(selected instanceof Entity) {
                        sprintService.selectRelease((Entity)selected);
                    }
                }
            }
        });
    }

    @Override
    public List<ComboItem> load() {
        List<ComboItem> items = new ArrayList<ComboItem>();

        EntityList releases = sprintService.getReleases();
        Entity selectedRelease = sprintService.getRelease();
        long now = System.currentTimeMillis();
        for(Entity release: releases) {
            if(SprintService.distance(now, release) == 0 || release.equals(selectedRelease)) {
                items.add(new ComboItem(release, release.getPropertyValue("name")));
            }
        }
        if(items.size() < releases.size() || releases.getTotal() > releases.size()) {
            items.add(new EntityChooser(entityService, "release", "Select release..."));
        }
        return items;
    }

    @Override
    public void onReleaseSelected(Entity release) {
        selectOrAddEntity(release);
    }

    @Override
    public void onSprintSelected(Entity sprint) {
    }

    @Override
    public void onTeamSelected(Entity team) {
    }

}
