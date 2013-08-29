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

package com.hp.alm.ali.idea.model.type;

import com.hp.alm.ali.idea.entity.EntityFilter;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.model.Entity;

import java.util.HashMap;
import java.util.Map;

public class Context<E extends EntityFilter<E>> {

    private Entity masterEntity;
    private Entity entity;
    private EntityEditor entityEditor;
    private E entityQuery;
    private Map<String, String> cache = new HashMap<String, String>();

    public Context(Entity entity, EntityEditor entityEditor) {
        this.entity = entity;
        this.entityEditor = entityEditor;
    }

    public Context(E entityQuery) {
        this.entityQuery = entityQuery;
    }

    public Context(Entity entity) {
        this.entity = entity;
    }

    public void setMasterEntity(Entity entity) {
        this.masterEntity = entity;
    }

    public Entity getMasterEntity() {
        return masterEntity;
    }

    public Entity getEntity() {
        return entity;
    }

    public E getEntityQuery() {
        return entityQuery;
    }

    public EntityEditor getEntityEditor() {
        return entityEditor;
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }

    public String get(String key) {
        return cache.get(key);
    }
}
