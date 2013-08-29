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

package com.hp.alm.ali.idea.entity;

import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;

import java.util.List;

public class EntityQueryProvider implements ItemsProvider<Entity> {
    private EntityService entityService;
    private String entityType;

    public EntityQueryProvider(EntityService entityService, String entityType) {
        this.entityService = entityService;
        this.entityType = entityType;
    }

    @Override
    public boolean load(String filter, List<Entity> items) {
        EntityQuery query = new EntityQuery(entityType);
        if(filter != null && !filter.isEmpty()) {
            query.setValue("name", "'"+filter+"'");
        }
        EntityList list = entityService.query(query);
        items.addAll(list);
        return list.getTotal() > list.size();
    }
}
