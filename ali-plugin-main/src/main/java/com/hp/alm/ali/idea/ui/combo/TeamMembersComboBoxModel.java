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

package com.hp.alm.ali.idea.ui.combo;

import com.hp.alm.ali.idea.content.taskboard.AssignedToComboBox;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.services.TeamMemberService;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import javax.swing.DefaultComboBoxModel;

public class TeamMembersComboBoxModel extends DefaultComboBoxModel implements LazyComboBoxModel {
    private boolean ready;

    public TeamMembersComboBoxModel(Project project) {
        project.getComponent(TeamMemberService.class).getTeamMembersAsync(project.getComponent(SprintService.class).getTeam(), new AbstractCachingService.DispatchCallback<EntityList>() {
            @Override
            public void loaded(EntityList members) {
                for(Entity member: members) {
                    ComboItem comboItem = new ComboItem(member.getPropertyValue("name"), member.getPropertyValue("full-name"));
                    if(getIndexOf(comboItem) < 0) {
                        addElement(comboItem);
                    }
                }
                if(getIndexOf(AssignedToComboBox.UNASSIGNED) < 0) {
                    addElement(AssignedToComboBox.UNASSIGNED);
                }
                setReady(true);
                fireContentsChanged(this, 0, getSize() - 1);
            }
        });
    }

    @Override
    public synchronized boolean isReady() {
        return ready;
    }

    public synchronized void setReady(boolean ready) {
        this.ready = ready;
    }
}
