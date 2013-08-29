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
import com.hp.alm.ali.idea.content.AliContentManager;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import javax.swing.SortOrder;

public class TeamMemberService extends AbstractCachingEntityService<Entity> {

    private EntityService entityService;

    public TeamMemberService(Project project, EntityService entityService) {
        super(project);

        this.entityService = entityService;
    }

    public EntityList getTeamMembers(Entity team) {
        return getValue(team);
    }

    public void getTeamMembersAsync(Entity team, Callback<EntityList> callback) {
        getValueAsync(team, callback);
    }

    protected EntityList doGetValue(Entity team) {
        AliContentManager.assertNotDispatchThread();

        EntityQuery query = new EntityQuery("team-member");
        query.addColumn("id", 1);
        query.addColumn("name", 1);
        query.addColumn("full-name", 1);
        query.setValue("team-id", team.getPropertyValue("id"));
        query.setPropertyResolved("team-id", true);
        query.addOrder("name", SortOrder.ASCENDING);
        return entityService.query(query);
    }
}
