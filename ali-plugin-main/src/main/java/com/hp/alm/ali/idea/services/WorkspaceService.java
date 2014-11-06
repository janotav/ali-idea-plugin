/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

import com.hp.alm.ali.idea.cfg.WorkspaceConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkspaceService extends AbstractCachingEntityService<Void> {

    private EntityService entityService;
    private RestService restService;
    private WorkspaceConfiguration workspaceConfiguration;

    public WorkspaceService(Project project, EntityService entityService, RestService restService, WorkspaceConfiguration workspaceConfiguration) {
        super(project);
        this.entityService = entityService;
        this.restService = restService;
        this.workspaceConfiguration = workspaceConfiguration;
    }

    public int getWorkspaceId() {
        return workspaceConfiguration.getWorkspaceId();
    }

    public String getWorkspaceName() {
        return workspaceConfiguration.getWorkspaceName();
    }

    public void selectWorkspace(int id, String name) {
        if (!Integer.valueOf(id).equals(workspaceConfiguration.getWorkspaceId())) {
            workspaceConfiguration.setWorkspaceId(id);
            workspaceConfiguration.setWorkspaceName(name);
            restService.fireServerTypeEvent();
        }
    }

    public Map<Integer, String> listWorkspaces() {
        return toWorkspaceMap(getValue(null));
    }

    public void listWorkspacesAsync(Callback<Map<Integer, String>> callback) {
        getValueAsync(null, translate(callback, new Transform<EntityList, Map<Integer, String>>() {
            @Override
            public Map<Integer, String> transform(EntityList entities) {
                return toWorkspaceMap(entities);
            }
        }));
    }

    @Override
    protected EntityList doGetValue(Void key) {
        return entityService.query(new EntityQuery("product-group"));
    }

    private Map<Integer, String> toWorkspaceMap(List<Entity> list) {
        HashMap<Integer, String> ret = new HashMap<Integer, String>();
        for (Entity entity: list) {
            ret.put(entity.getId(), entity.getPropertyValue("name"));
        }
        return ret;
    }
}
