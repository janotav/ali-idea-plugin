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
import com.intellij.openapi.project.Project;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

public class SprintComboBox extends LazyComboBox implements SprintService.Listener {

    private SprintService sprintService;

    public SprintComboBox(Project project) {
        super(project, "sprint");

        sprintService = project.getComponent(SprintService.class);
        sprintService.addListener(this);

        Entity sprint = sprintService.getSprint();
        if(sprint != null) {
            addItem(new ComboItem(sprint, sprint.getPropertyValue("name")));
        }

        addItemListener(new NonLoadingItemListener() {
            @Override
            public void doItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object sprint = ((ComboItem) e.getItem()).getKey();
                    if (sprint instanceof Entity) {
                        sprintService.selectSprint((Entity) sprint);
                    }
                }
            }
        });
    }

    @Override
    public List<ComboItem> load() {
        List<ComboItem> items = new ArrayList<ComboItem>();

        List<Entity> sprints = sprintService.getSprints();
        if(sprints != null) {
            for(Entity sprint: sprints) {
                items.add(new ComboItem(sprint, sprint.getPropertyValue("name")));
            }
        }

        return items;
    }

    @Override
    public void onReleaseSelected(Entity release) {
        reload();
    }

    @Override
    public void onSprintSelected(Entity sprint) {
        selectOrAddEntity(sprint);
    }

    @Override
    public void onTeamSelected(Entity team) {
    }
}
