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

import com.hp.alm.ali.idea.entity.DummyStatusIndicator;
import com.hp.alm.ali.idea.entity.EntityFilterModel;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.EntityStatusIndicator;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.entity.queue.QueryTarget;
import com.hp.alm.ali.idea.entity.queue.QueryQueue;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.model.type.ContextAware;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.entity.CachingEntityListener;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.NotConnectedException;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityTableModel extends AbstractTableModel implements Disposable, MetadataService.DispatchMetadataCallback, CachingEntityListener, ServerTypeListener, QuerySharingManager.QuerySharing, EntityFilterModel<EntityQuery>,QueryTarget {

    private TableColumnModel columnModel;
    private boolean autoload;
    private RowSorter rowSorter;
    private RestService restService;
    private EntityStatusIndicator status;
    private Metadata meta;
    private String entityName;
    private Set<String> hiddenFields;
    private EntityQueryProcessor processor;
    private MetadataService metadataService;
    private EntityService entityService;
    private TranslateService translateService;
    private boolean ignoreSetOrder = false;
    private EntityMatcher matcher = new FieldBasedMatcher();

    // instance obtained from AliConfiguration that is automatically persisted
    final private EntityQuery query;
    final private List<FilterListener> queryListeners = new LinkedList<FilterListener>();

    final private List<Field> fields = new ArrayList<Field>();
    private EntityList data = EntityList.empty();
    private QueryQueue queue;
    private boolean forceQuery = false;
    private Map<Entity, Context> contextMap = new HashMap<Entity, Context>();

    public EntityTableModel(Project project, TableColumnModel columnModel, boolean autoload, String entityName, EntityQuery query, MyRowSorter rowSorter, Set<String> hiddenFields, EntityQueryProcessor processor) {
        this.autoload = autoload;
        this.rowSorter = rowSorter;
        this.columnModel = columnModel;
        this.entityName = entityName;
        this.hiddenFields = hiddenFields;
        this.processor = processor;
        this.restService = project.getComponent(RestService.class);
        this.metadataService = project.getComponent(MetadataService.class);
        this.query = query;
        entityService = project.getComponent(EntityService.class);
        translateService = project.getComponent(TranslateService.class);
        status = new DummyStatusIndicator();
        queue = new QueryQueue(project, status, true, this);

        entityService.addEntityListener(this);
        restService.addServerTypeListener(this);

        rowSorter.setModel(this);
        loadMetaData();
    }

    private void loadMetaData() {
        try {
            status.loading();
            metadataService.loadEntityMetadataAsync(entityName, this);
        } catch(NotConnectedException e) {
            status.info("Not connected", null, null);
        }
    }

    public void dispose() {
    }

    @Override
    public void connectedTo(final ServerType serverType) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if(!serverType.isConnected() || !autoload) {
                    data = EntityList.empty();
                    meta = null;
                    status.info("Disconnected", null, null);
                    fireTableDataChanged();
                } else {
                    loadMetaData();
                }
            }
        });
    }

    public String getColumnName(int col) {
        return fields.get(col).getLabel();
    }

    public Class getColumnClass(int col) {
        Field field = fields.get(col);
        if(translateService.isTranslated(field)) {
            return Translator.class;
        } else {
            return field.getClazz();
        }
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return fields.size();
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void columnRemovedFromView(int col) {
        Field field = fields.get(col);
        query.removeView(field.getName());
        fireFilterChangedEvent();
    }

    public Object getValueAt(int row, int col) {
        String property = colToProperty(col);
        final Entity entity = data.get(row);
        Object value = entity.getProperty(property);
        if(value != null) {
            Class clazz = getColumnClass(col);
            if(Integer.class.equals(clazz)) {
                return Integer.valueOf(value.toString());
            }
        }
        return value;
    }

    public Entity getEntity(int row) {
        return data.get(row);
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void setFilter(String prop, String value) {
        if(query.setValue(prop, value)) {
            fireFilterChangedEvent();
            reload();
        }
    }

    public void setOrder(List<RowSorter.SortKey> keys) {
        if(ignoreSetOrder) {
            return;
        }

        LinkedHashMap<String, SortOrder> map = new LinkedHashMap<String, SortOrder>();
        for(RowSorter.SortKey key: keys) {
            String name = fields.get(key.getColumn()).getName();
            map.put(name, key.getSortOrder());
        }
        query.setOrder(map);

        fireFilterChangedEvent();
        reload();
    }

    public void fireTableStructureChanged() {
        LinkedHashMap<String, Integer> columns = query.getColumns();
        while(columnModel.getColumnCount() > 0) {
            columnModel.removeColumn(columnModel.getColumn(0));
        }
        for(String name: columns.keySet()) {
            Field field = meta.getAllFields().get(name);
            TableColumn column = new TableColumn(fields.indexOf(field), columns.get(name));
            column.setHeaderValue(field.getLabel());
            columnModel.addColumn(column);
        }
        List<RowSorter.SortKey> keys = new LinkedList<RowSorter.SortKey>();
        for(Map.Entry<String, SortOrder> key: query.getOrder().entrySet()) {
            keys.add(new RowSorter.SortKey(fields.indexOf(meta.getAllFields().get(key.getKey())), key.getValue()));
        }
        List sortKeys = rowSorter.getSortKeys();
        ignoreSetOrder = true;
        try {
            super.fireTableStructureChanged();
        } finally {
            ignoreSetOrder = false;
        }
        if(!sortKeys.equals(keys)) {
            // restore visual sort indicator
            rowSorter.setSortKeys(keys);
        } else if(forceQuery) {
            reload();
        }
    }

    public void updateColumnsFromView() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        for(int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            map.put(fields.get(column.getModelIndex()).getName(), column.getWidth());
        }
        query.setColumns(map);
        fireFilterChangedEvent();
    }

    private String colToProperty(int col) {
        return fields.get(col).getName();
    }

    public static List<String> quote(List<String> list) {
        if(list != null) {
            LinkedList<String> quoted = new LinkedList<String>();
            List<String> ret = list;
            for(String value: list) {
                quoted.add("'"+value+"'");
                if(value.contains(" ")) {
                    // if at least one value needs quote, quote all to provide reasonable sort
                    ret = quoted;
                }
            }
            return ret;
        } else {
            return null;
        }
    }

    public void reload() {
        if(meta == null) {
            loadMetaData();
        } else {
            queryALM(query);
        }
    }

    private void queryALM(final EntityQuery query) {
        final EntityQuery clone;
        if(processor != null) {
            clone = processor.preProcess(query.clone());
        } else {
            clone = query.clone();
        }
        forceQuery = false;
        queue.query(clone);
    }

    public void setFilter(EntityQuery filter) {
        this.query.copyFrom(filter);
        if(meta != null) {
            this.query.purgeInvalid(meta);
        }

        fireFilterUpdated(true);
    }

    @Override
    public EntityQuery getFilter() {
        return query;
    }

    @Override
    public void fireFilterUpdated(boolean dataChanged) {
        if(dataChanged) {
            forceQuery = true;
        }
        fireFilterChangedEvent();
        fireTableStructureChanged();
    }

    public void metadataLoaded(Metadata metadata) {
        this.meta = metadata;

        // populate model
        fields.clear();
        fields.addAll(meta.getAllFields().values());

        // no columns specified, load defaults based on server type
        if(query.getColumns().isEmpty()) {
            query.setColumns(restService.getModelCustomization().getDefaultTableFilter(entityName).getColumns());
        }

        // remove non-existing fields from filter
        query.purgeInvalid(meta);

        // restore columns into view
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                fireTableStructureChanged();
            }
        });

        reload();
    }

    public void metadataFailed() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                status.info("Failed to load metadata", null, new Runnable() {
                    public void run() {
                        loadMetaData();
                    }
                });
            }
        });
    }

    @Override
    public void entityLoaded(final Entity entity, final Event event) {
        if(entityName.equals(entity.getType())) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    int i = data.indexOf(entity);
                    if(i >= 0) {
                        data.set(i, entity);
                        fireTableRowsUpdated(i, i);
                    } else if(event == Event.CREATE && matcher.matches(entity)) {
                        data.add(entity);
                        fireTableRowsInserted(data.size() - 1, data.size() - 1);
                    }
                }
            });
        }
    }

    @Override
    public void entityNotFound(final EntityRef ref, boolean removed) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                int i = data.indexOf(ref.toEntity());
                if(i >= 0) {
                    Entity entity = data.remove(i);
                    contextMap.remove(entity);
                    fireTableRowsDeleted(i, i);
                } else if(entityName.equals("release-backlog-item")) {
                    for(int j = 0; j < data.size(); j++) {
                        Entity entity = data.get(j);
                        if(entity.getPropertyValue("entity-type").equals(ref.type) && entity.getPropertyValue("entity-id").equals(String.valueOf(ref.id))) {
                            Entity entity1 = data.remove(i);
                            contextMap.remove(entity1);
                            fireTableRowsDeleted(j, j);
                            break;
                        }
                    }
                }
            }
        });
    }

    public Entity lookup(final EntityRef ref) {
        final LinkedList<Entity> ret = new LinkedList<Entity>();
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            public void run() {
                int i = data.indexOf(new Entity(ref.type, ref.id));
                if(i >= 0) {
                    ret.add(data.get(i));
                }
            }
        });
        if(ret.isEmpty()) {
            return null;
        } else {
            return ret.get(0);
        }
    }

    @Override
    public void addFilterListener(FilterListener listener) {
        synchronized (queryListeners) {
            queryListeners.add(listener);
        }
    }

    public void fireFilterChangedEvent() {
        synchronized (queryListeners) {
            for(FilterListener listener: queryListeners) {
                listener.filterChanged(query);
            }
        }
    }

    public void setStatusIndicator(EntityStatusIndicator status) {
        this.status = status;
        queue.setStatusIndicator(status);
    }

    public int indexOf(Entity entity) {
        return data.indexOf(entity);
    }

    @Override
    public void setColumns(LinkedHashMap<String, Integer> columns) {
        HashSet<String> newColumns = new HashSet<String>(columns.keySet());
        newColumns.removeAll(query.getColumns().keySet());
        if (!newColumns.isEmpty()) {
            // adding new column into query, must reload
            forceQuery = true;
        }
        query.setColumns(columns);
        fireFilterChangedEvent();
        fireTableStructureChanged();
    }

    public Set<String> getHiddenFields() {
        return hiddenFields;
    }

    public Translator getTranslator(Field field, int row, Entity masterEntity) {
        Translator translator = translateService.getTranslator(field);
        if(translator instanceof ContextAware) {
            Entity entity = getEntity(row);
            Context context = contextMap.get(entity);
            if(context == null) {
                context = new Context(entity);
                context.setMasterEntity(masterEntity);
                contextMap.put(entity, context);
            }
            ((ContextAware) translator).setContext(context);
        }
        return translator;
    }

    public void setMatcher(EntityMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void handleResult(final EntityList list) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                data = list;
                fireTableDataChanged();
            }
        });
    }

    public interface EntityMatcher {

        boolean matches(Entity entity);

    }

    public class FieldBasedMatcher implements EntityMatcher {

        @Override
        public boolean matches(Entity entity) {
            for(String field: hiddenFields) {
                if(field.startsWith("virtual:")) {
                    continue;
                }
                if(!entity.getPropertyValue(field).equals(getFilter().getValue(field))) {
                    return false;
                }
            }
            return true;
        }
    }
}
