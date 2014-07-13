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

package com.hp.alm.ali.idea.content.taskboard;

import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.services.TeamMemberService;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.LazyComboBox;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class AssignedToComboBox extends LazyComboBox implements SprintService.Listener {

    private SprintService sprintService;
    private TeamMemberService teamMemberService;
    public static ComboItem ALL = new ComboItem("<All>");
    public static ComboItem UNASSIGNED = new ComboItem("", "<Unassigned>");

    public AssignedToComboBox(Project project, String current) {
        super(project, "team member");

        this.teamMemberService = project.getComponent(TeamMemberService.class);
        this.sprintService = project.getComponent(SprintService.class);
        sprintService.addListener(this);

        if(current != null && !current.isEmpty()) {
            addItem(new ComboItem(current));
            setSelectedIndex(0);
        }
    }

    @Override
    public List<ComboItem> load() {
        List<ComboItem> items = new ArrayList<ComboItem>();
        items.add(ALL);
        items.add(UNASSIGNED);

        Entity team = sprintService.getTeam();
        if(team == null) {
            return items;
        }

        EntityList members = teamMemberService.getTeamMembers(team);
        for(Entity member: members) {
            items.add(new ComboItem(member.getPropertyValue("name"), member.getPropertyValue("full-name")));
        }
        return items;
    }

    @Override
    public void onReleaseSelected(Entity release) {
    }

    @Override
    public void onSprintSelected(Entity sprint) {
    }

    @Override
    public void onTeamSelected(Entity team) {
        reload();
    }
}
