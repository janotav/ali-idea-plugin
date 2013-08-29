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

package com.hp.alm.ali.idea.entity.table;

import com.hp.alm.ali.idea.entity.EntityFilterModel;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.services.WeakListeners;
import org.apache.commons.lang.mutable.MutableBoolean;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class QuerySharingManager {

    private Map<String, WeakListeners<FilterListener<EntityQuery>>> listeners = new HashMap<String, WeakListeners<FilterListener<EntityQuery>>>();
    private WeakHashMap<QuerySharing, FilterListener<EntityQuery>> ref = new WeakHashMap<QuerySharing, FilterListener<EntityQuery>>();
    private AliProjectConfiguration conf;

    public QuerySharingManager(AliProjectConfiguration conf) {
        this.conf = conf;
    }

    public void addSharedQuery(final QuerySharing model, final String id) {
        final MutableBoolean ignore = new MutableBoolean(false);
        model.addFilterListener(new FilterListener<EntityQuery>() {
            @Override
            public void filterChanged(EntityQuery query) {
                if (!ignore.booleanValue()) {
                    fireChangedEvent(query, id);
                }
            }
        });
        FilterListener<EntityQuery> listener = new FilterListener<EntityQuery>() {
            @Override
            public void filterChanged(EntityQuery query) {
                LinkedHashMap<String, Integer> columns = query.getColumns();
                if (query == model.getFilter()) {
                    conf.getFilter(id).setColumns(columns);
                    return; // ignore our own event (only store in project configuration)
                }

                ignore.setValue(true);
                try {
                    model.setColumns(columns);
                } finally {
                    ignore.setValue(false);
                }
            }
        };
        addListener(id, listener);
        ref.put(model, listener); // make sure the listener is not garbage collected
    }

    public void removeSharedQuery(QuerySharing model, final String id) {
        FilterListener<EntityQuery> listener = ref.remove(model);
        if(listener != null) {
            getOrCreate(id).remove(listener);
        }
    }

    private void addListener(String id, FilterListener<EntityQuery> listener) {
        getOrCreate(id).add(listener);
    }

    private void fireChangedEvent(final EntityQuery query, String id) {
        getOrCreate(id).fire(new WeakListeners.Action<FilterListener<EntityQuery>>() {
            @Override
            public void fire(FilterListener<EntityQuery> listener) {
                listener.filterChanged(query);
            }
        });
    }

    private synchronized WeakListeners<FilterListener<EntityQuery>> getOrCreate(String queryId) {
        WeakListeners<FilterListener<EntityQuery>> entityListeners = listeners.get(queryId);
        if(entityListeners == null) {
            entityListeners = new WeakListeners<FilterListener<EntityQuery>>();
            listeners.put(queryId, entityListeners);
        }
        return entityListeners;
    }

    interface QuerySharing extends EntityFilterModel<EntityQuery> {

        void setColumns(LinkedHashMap<String, Integer> columns);

    }
}
