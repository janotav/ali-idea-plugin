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

import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.parser.FavoritesList;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.translate.expr.ExpressionParser;
import com.hp.alm.ali.idea.translate.expr.Node;
import com.hp.alm.ali.idea.translate.expr.Type;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoritesService extends AbstractCachingService<FavoritesService.Favorite, EntityQuery, AbstractCachingService.Callback<EntityQuery>> {

    private EntityService entityService;
    private AliProjectConfiguration projConf;
    private MetadataService metadataService;
    private FilterManager filterManager;
    private RestService restService;
    private AliConfiguration aliConfiguration;

    public FavoritesService(Project project, EntityService entityService, AliProjectConfiguration projConf, MetadataService metadataService, FilterManager filterManager, RestService restService) {
        super(project);
        this.entityService = entityService;
        this.projConf = projConf;
        this.metadataService = metadataService;
        this.filterManager = filterManager;
        this.restService = restService;
        aliConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
    }

    public EntityQuery getFavorite(int id, String entityType) {
        return getValue(new Favorite(id, entityType));
    }

    public EntityQuery getStoredQuery(String entityType, String queryNameAndKind) {
        Matcher matcher = Pattern.compile("^(\\d+): .* \\(ALM\\)$").matcher(queryNameAndKind);
        if(matcher.matches()) {
            // server favorite
            int id = Integer.valueOf(matcher.group(1));
            return getFavorite(id, entityType);
        } else {
            // local favorite
            for(EntityQuery filter: getAvailableQueries(entityType)) {
                if(queryNameAndKind.equals(filter.getName())) {
                    return filter;
                }
            }
            return null;
        }
    }

    public List<EntityQuery> getAvailableQueries(String entityType) {
        LinkedList<EntityQuery> list = new LinkedList<EntityQuery>();
        process(list, projConf.getStoredFilters(entityType), "project");
        process(list, aliConfiguration.getStoredFilters(entityType), "global");
        return list;
    }

    @Override
    protected EntityQuery doGetValue(Favorite key) {
        EntityQuery favoriteQuery = new EntityQuery("favorite");
        favoriteQuery.setValue("id", String.valueOf(key.id));
        InputStream is = entityService.queryForStream(favoriteQuery);
        FavoritesList list = FavoritesList.create(is, key.entityType);
        if(!list.isEmpty()) {
            EntityQuery query = (EntityQuery) list.get(0).getProperty("query");
            unresolveQuery(query);
            return query;
        } else {
            return null;
        }
    }

    /**
     * Convert the query in the REST expression format (e.g. 'New OR Open OR Closed') to the format suitable for given
     * filter factory (e.g. 'New;Open;Closed'). This conversion needs to be performed on the server-side favorites
     * that are using REST expressions and would yield invalid results if applied directly.
     *
     * Stored queries may also reference fields that we mask out (e.g. owner), use the appropriate alias in
     * that case (e.g. release-backlog-item.owner).
     *
     * @param query entity query parsed from server favorites
     */
    public void unresolveQuery(EntityQuery query) {
        for(String property: query.getPropertyMap().keySet()) {
            String realProperty = restService.getServerStrategy().getFieldAlias(query.getEntityType(), property);
            if(!property.equals(realProperty)) {
                query.setValue(realProperty, query.getValue(property));
                query.setValue(property, null);
            }
            Field field = metadataService.getEntityMetadata(query.getEntityType()).getField(realProperty);
            FilterFactory factory = filterManager.getFilterFactory(new Context<EntityQuery>(query), query.getEntityType(), field, true);
            if(factory != null) {
                Node node = ExpressionParser.parse(query.getValue(realProperty));
                List<String> values = new LinkedList<String>();
                if(collectOrValues(node, values)) {
                    query.setValue(realProperty, factory.multipleValues(values));
                }
            }
        }
    }

    private boolean collectOrValues(Node node, List<String> values) {
        if(node.type == Type.OR) {
            return collectOrValues(node.left, values) && collectOrValues(node.right, values);
        } else if(node.type == Type.VALUE) {
            values.add(node.value);
            return true;
        } else {
            return false;
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

    static class Favorite {
        private int id;
        private String entityType;

        public Favorite(int id, String entityType) {
            this.id = id;
            this.entityType = entityType;
        }
    }
}
