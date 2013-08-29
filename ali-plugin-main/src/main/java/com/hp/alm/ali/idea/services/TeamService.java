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
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import java.util.List;

public class TeamService extends AbstractCachingEntityService<String> {

    private EntityService entityService;

    public TeamService(Project project, EntityService entityService) {
        super(project);

        this.entityService = entityService;
    }

    public Entity getTeam(String teamName, int releaseId) {
        List<Entity> teams = getTeams(teamName);
        for(Entity team: teams) {
            if(team.getPropertyValue("release-id").equals(String.valueOf(releaseId))) {
                return team;
            }
        }
        return null;
    }

    public List<Entity> getTeams(String teamName) {
        return getValue(teamName);
    }

    public EntityList getMultipleTeams(List<String> teamNames) {
        // TODO: make single query (needs additional support in the caching service)
        EntityList entities = EntityList.empty();
        for(String teamName: teamNames) {
            entities.addAll(getTeams(teamName));
        }
        return entities;
    }

    @Override
    protected EntityList doGetValue(String teamName) {
        EntityQuery query = new EntityQuery("team");
        query.setValue("name", "'" + teamName + "'");
        return entityService.query(query);
    }
}
