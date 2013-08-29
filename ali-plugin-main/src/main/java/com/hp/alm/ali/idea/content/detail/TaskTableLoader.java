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

package com.hp.alm.ali.idea.content.detail;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.Arrays;

public class TaskTableLoader implements TableContent.EntityTableLoader {

    private Project project;
    private Entity entity;
    private EntityService entityService;
    private EntityTable entityTable;

    public TaskTableLoader(Project project, Entity entity) {
        this.project = project;
        this.entity = entity;
        entityService = project.getComponent(EntityService.class);
    }

    @Override
    public void load(final TableContent tableContent) {
        if(entity.isInitialized("release-backlog-item.id")) {
            load(entity, tableContent);
        } else {
            entityService.requestCachedEntity(new EntityRef(entity), Arrays.asList("release-backlog-item.id"), new EntityListener() {
                @Override
                public void entityLoaded(Entity entity, Event event) {
                    load(entity, tableContent);
                }

                @Override
                public void entityNotFound(EntityRef ref, boolean removed) {
                    tableContent.setTable(null);
                }
            });
        }
    }

    private void load(Entity entity, TableContent tableContent) {
        if(entityTable == null) {
            EntityQuery taskQuery = new EntityQuery("project-task");
            taskQuery.setValue("release-backlog-item-id", entity.getPropertyValue("release-backlog-item.id"));
            EntityQuery query = project.getComponent(AliProjectConfiguration.class).getFilter(taskQuery.getEntityType());
            taskQuery.setColumns(query.getColumns());
            entityTable = new EntityTable(project, taskQuery.getEntityType(), false, taskQuery, taskQuery.getPropertyMap().keySet(), null, false, entity);
        } else {
            // refresh existing UI table to avoid flicker
            entityTable.getModel().getFilter().setValue("release-backlog-item-id", entity.getPropertyValue("release-backlog-item.id"));
        }
        tableContent.setTable(entityTable);
    }
}
