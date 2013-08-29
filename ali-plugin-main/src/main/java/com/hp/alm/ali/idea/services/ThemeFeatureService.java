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
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import javax.swing.SortOrder;

public class ThemeFeatureService extends AbstractCachingEntityService<ThemeFeatureService.Type> {

    public static enum Type {

        feature(71), theme(72);

        private int id;

        private Type(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private EntityService entityService;

    public ThemeFeatureService(Project project, EntityService entityService) {
        super(project);

        this.entityService = entityService;
    }

    public EntityList getFeatures() {
        return getValue(Type.feature);
    }

    public EntityList getFeatures(String filter) {
        if(filter == null || filter.isEmpty()) {
            return getFeatures();
        } else {
            return doGetValue(Type.feature, filter);
        }
    }

    public EntityList getThemes() {
        return getValue(Type.theme);
    }

    public EntityList getThemes(String filter) {
        if(filter == null || filter.isEmpty()) {
            return getThemes();
        } else {
            return doGetValue(Type.theme, filter);
        }
    }

    protected EntityList doGetValue(Type type) {
        return doGetValue(type, null);
    }

    private EntityList doGetValue(Type type, String filter) {
        AliContentManager.assertNotDispatchThread();

        EntityQuery query = new EntityQuery("requirement");
        query.addColumn("id", 1);
        query.addColumn("name", 1);
        query.setValue("type-id", String.valueOf(type.getId()));
        if(filter != null && !filter.isEmpty()) {
            query.setValue("name", filter);
        }
        query.addOrder("name", SortOrder.ASCENDING);
        query.setPageSize(1000);
        return entityService.query(query);
    }
}
