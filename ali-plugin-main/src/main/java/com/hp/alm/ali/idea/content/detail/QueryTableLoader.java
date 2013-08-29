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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.LinkedHashMap;
import java.util.Set;

public class QueryTableLoader implements TableContent.EntityTableLoader {

    protected final Project project;
    private Entity entity;
    private final EntityQuery query;
    private final Set<String> hiddenFields;
    private EntityTable entityTable;

    public QueryTableLoader(Project project, Entity entity, EntityQuery query, Set<String> hiddenFields) {
        this.project = project;
        this.entity = entity;
        this.query = query;
        this.hiddenFields = hiddenFields;
    }

    @Override
    public void load(TableContent content) {
        if(entityTable == null) {
            LinkedHashMap<String,Integer> columns = project.getComponent(AliProjectConfiguration.class).getFilter(query.getEntityType()).getColumns();
            query.setColumns(columns);
            entityTable = new EntityTable(project, query.getEntityType(), false, query, hiddenFields, null, false, entity);
            onTableCreated(entityTable);
        }
        content.setTable(entityTable);
    }

    protected void onTableCreated(EntityTable entityTable) {
    }
}
