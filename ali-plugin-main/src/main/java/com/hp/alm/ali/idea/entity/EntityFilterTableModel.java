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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.model.Metadata;
import com.intellij.openapi.project.Project;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityFilterTableModel<E extends EntityFilter<E>> extends AbstractTableModel {
    private ArrayList<Field> fields;
    private E filter;
    private TranslateService translateService;
    private Map<Integer, String> values;

    public EntityFilterTableModel(Project project, Metadata metadata, E filter, Set<String> hiddenFields) {
        this.filter = filter;
        translateService = project.getComponent(TranslateService.class);

        values = new HashMap<Integer, String>();
        fields = filterable(metadata.getAllFields().values(), hiddenFields);
    }

    public List<Field> getFields() {
        return fields;
    }

    public int getRowCount() {
        return fields.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public String getUserFilter(int row) {
        Field field = fields.get(row);
        return filter.getValue(field.getName());
    }

    public Object getValueAt(final int row, final int col) {
        Field field = fields.get(row);
        if(col == 0) {
            return field.getLabel();
        } else {
            String userFilter = getUserFilter(row);
            if(userFilter == null || userFilter.isEmpty()) {
                return userFilter;
            }
            String value = values.get(row);
            if(value == null) {
                return translateService.convertQueryModelToView(field, userFilter, new ValueCallback.Dispatch(new ValueCallback() {
                    @Override
                    public void value(String value) {
                        values.put(row, value);
                        fireTableRowsUpdated(row, row); // row/cell update does not work
                    }
                }));
            }
            return value;
        }
    }

    // used by default cell editor
    public void setValueAt(Object value, int row, int col) {
        if(col == 1) {
            setUserFilter(row, (String) value);
        } else {
            throw new IllegalArgumentException("col == " + col);
        }
    }

    public void setUserFilter(int row, String value) {
        Field field = fields.get(row);
        filter.setValue(field.getName(), value);
        values.remove(row);
        fireTableCellUpdated(row, 1);
    }

    public String getColumnName(int i) {
        if(i == 0) {
            return "Property";
        } else {
            return "Condition";
        }
    }

    public boolean isCellEditable(int row, int col) {
        return col == 1;
    }

    private ArrayList<Field> filterable(Collection<Field> fields, Set<String> hiddenFields) {
        ArrayList<Field> list = new ArrayList<Field>();
        for(Field field: fields) {
            if(field.isCanFilter() && !hiddenFields.contains(field.getName())) {
                list.add(field);
            }
        }
        return list;
    }
}
