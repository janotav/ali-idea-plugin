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

package com.hp.alm.ali.idea.model;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.model.parser.FavoritesList;
import com.hp.alm.ali.idea.services.EntityService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Favorites {

    private static Map<String, Integer> moduleMap;
    static {
        moduleMap = new HashMap<String, Integer>();
        moduleMap.put("defect", 0);
        moduleMap.put("requirement", 3);
        moduleMap.put("release-backlog-item", 150);
    }

    private AliProjectConfiguration projConf;
    private EntityService entityService;
    final private String entityType;

    private List<EntityQuery> storedFilters;
    private List<EntityQuery> globalFilters;
    private Map<Integer, EntityQuery> serverFilters;

    public Favorites(final Project project, String entityType) {
        this.entityType = entityType;

        projConf = project.getComponent(AliProjectConfiguration.class);
        entityService = project.getComponent(EntityService.class);
        storedFilters = projConf.getStoredFilters(entityType);
        globalFilters = ApplicationManager.getApplication().getComponent(AliConfiguration.class).getStoredFilters(entityType);
        serverFilters = new HashMap<Integer, EntityQuery>();
    }

    public List<EntityQuery> getAvailableFilters() {
        LinkedList<EntityQuery> list = new LinkedList<EntityQuery>();
        process(list, storedFilters, "project");
        process(list, globalFilters, "global");
        return list;
    }

    public List<EntityQuery> getStoredFilters() {
        return Collections.unmodifiableList(storedFilters);
    }

    public List<EntityQuery> getGlobalFilters() {
        return Collections.unmodifiableList(globalFilters);
    }

    public synchronized EntityQuery findStoredQuery(String queryNameAndKind) {
        Matcher matcher = Pattern.compile("^(\\d+): .* \\(ALM\\)$").matcher(queryNameAndKind);
        if(matcher.matches()) {
            // server favorite
            Integer id = Integer.valueOf(matcher.group(1));
            EntityQuery filter = serverFilters.get(id);
            if(filter == null) {
                EntityQuery favoriteQuery = new EntityQuery("favorite");
                favoriteQuery.setValue("id", id.toString());
                InputStream is = entityService.queryForStream(favoriteQuery);
                FavoritesList list = FavoritesList.create(is, entityType);
                if(!list.isEmpty()) {
                    filter = (EntityQuery)list.get(0).getProperty("query");
                    serverFilters.put(id, filter);
                }
            }
            return filter;
        } else {
            // local favorite
            for(EntityQuery filter: getAvailableFilters()) {
                if(queryNameAndKind.equals(filter.getName())) {
                    return filter;
                }
            }
            return null;
        }
    }

    private void process(LinkedList<EntityQuery> list, List<EntityQuery> filters, String kind) {
        for(EntityQuery filter: filters) {
            EntityQuery f = new EntityQuery(filter.getEntityType());
            f.copyFrom(filter);
            f.setName(filter.getName() + " (" + kind + ")");
            list.add(f);
        }
    }

    public synchronized void reloadStored() {
        storedFilters = projConf.getStoredFilters(entityType);
        globalFilters = ApplicationManager.getApplication().getComponent(AliConfiguration.class).getStoredFilters(entityType);
    }

    public String getEntityType() {
        return entityType;
    }

    public static int getModuleForEntityType(String entityType) {
        return moduleMap.get(entityType);
    }
}
