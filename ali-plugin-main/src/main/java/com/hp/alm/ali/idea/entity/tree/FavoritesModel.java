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

package com.hp.alm.ali.idea.entity.tree;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.parser.FavoritesList;
import com.hp.alm.ali.idea.services.FavoritesService;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesModel extends HierarchicalEntityModel {

    private static Map<String, Integer> moduleMap;
    static {
        moduleMap = new HashMap<String, Integer>();
        moduleMap.put("defect", 0);
        moduleMap.put("requirement", 3);
        moduleMap.put("release-backlog-item", 150);
    }

    private String targetType;
    private FavoritesService favoritesService;

    public FavoritesModel(Project project, String targetType) {
        super(project, "favorite", true, false);
        this.targetType = targetType;

        favoritesService = project.getComponent(FavoritesService.class);

        initRootEntity();
    }

    public List<Entity> queryForNodes(EntityQuery query) {
        query.setValue("module-id", "" + getModuleForEntityType(targetType));
        if("favorite".equals(query.getEntityType())) {
            query.removeView("parent-id"); // not supported by favorites, we have to ask for all fields
            InputStream is = entityService.queryForStream(query);
            FavoritesList list = FavoritesList.create(is, targetType);
            for(Entity favorite: list) {
                favoritesService.unresolveQuery((EntityQuery)favorite.getProperty("query"));
            }
            return list;
        } else {
            return super.queryForNodes(query);
        }
    }

    public List<Entity> getRootEntity() {
        EntityQuery query = new EntityQuery("favorite-folder");
        query.setValue("module-id", "" + getModuleForEntityType(targetType));
        query.setValue("parent-id", "-1");
        return entityService.query(query);
    }

    private static int getModuleForEntityType(String entityType) {
        return moduleMap.get(entityType);
    }
}
