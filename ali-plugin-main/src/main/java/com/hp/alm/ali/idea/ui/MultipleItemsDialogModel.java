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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.model.KeyValue;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.intellij.util.ui.UIUtil;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MultipleItemsDialogModel<K, E extends KeyValue<K>> extends AbstractTableModel {
    private String columnName;
    private boolean multiple;
    private ItemsProvider<E> provider;
    private List<E> allFields;
    private List<K> selectedFields;
    private boolean additional;
    private Translator translator;
    private boolean showingSelected;
    private boolean ignoreSelectionChange;

    public MultipleItemsDialogModel(String columnName, boolean multiple, ItemsProvider<E> provider, List<K> selectedFields, Translator translator) {
        this.columnName = columnName;
        this.multiple = multiple;
        this.provider = provider;
        this.selectedFields = selectedFields;
        this.translator = translator;
        allFields = new ArrayList<E>();
    }

    @Override
    public int getRowCount() {
        if(showingSelected) {
            return selectedFields.size();
        } else {
            return allFields.size();
        }
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Class getColumnClass(int col) {
        if(col == 0) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public Object getValueAt(final int row, int col) {
        if(showingSelected) {
            if(col == 0) {
                return true;
            } else {
                K key = selectedFields.get(row);
                for(E field: allFields) {
                    if(field.getKey().equals(key)) {
                        return field.getValue();
                    }
                }
                if(translator != null) {
                    return translator.translate(key.toString(), new ValueCallback() {
                        @Override
                        public void value(String value) {
                            fireTableRowsUpdated(row, row);
                        }
                    });
                }
                return key;
            }
        } else {
            KeyValue<K> field = allFields.get(row);
            if(col == 0) {
                return selectedFields.contains(field.getKey());
            } else {
                return field.getValue();
            }
        }
    }

    @Override
    public void setValueAt(Object o, int row, int col) {
        if(ignoreSelectionChange) {
            // keep selection when switching to/from showingSelected
        } else if(showingSelected) {
            if(Boolean.FALSE.equals(o)) {
                selectedFields.remove(row);
                // removal causes structural change, need to update whole table
                fireTableDataChanged();
            }
        } else {
            KeyValue<K> field = allFields.get(row);
            if(Boolean.TRUE.equals(o)) {
                if(!selectedFields.contains(field.getKey())) {
                    if(!isMultiple() && !selectedFields.isEmpty()) {
                        selectedFields.clear();
                        selectedFields.add(field.getKey());
                        fireTableRowsUpdated(0, allFields.size() - 1);

                    } else {
                        selectedFields.add(field.getKey());
                        fireTableRowsUpdated(row, row);
                    }
                }
            } else if(selectedFields.remove(field.getKey())) {
                fireTableRowsUpdated(row, row);
            }
            // NOTE: fireTableCellUpdated(row, col) is not enough if filter is specified for (un)selected fields
            // there is probably no reason to complicate things here, simply update whole table on every update...W
        }
    }

    @Override
    public String getColumnName(int col) {
        if(col == 0) {
            return "";
        } else {
            return columnName;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0;
    }

    public void fireTableDataChanged() {
        ignoreSelectionChange = true;
        try {
            super.fireTableDataChanged();
        } finally {
            ignoreSelectionChange = false;
        }
    }

    public void setShowingSelected(boolean showingSelected) {
        if(showingSelected != this.showingSelected) {
            this.showingSelected = showingSelected;
            fireTableDataChanged();
        }
    }

    public boolean isShowingSelected() {
        return showingSelected;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public List<K> getSelectedFields() {
        return selectedFields;
    }

    public List<E> getFields() {
        return allFields;
    }

    public void load(String filter) {
        final LinkedList<E> data = new LinkedList<E>();
        final boolean more = provider.load(filter, data);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                allFields.clear();
                allFields.addAll(data);
                additional = more;
                fireTableDataChanged();
            }
        });
    }

    public boolean hasMore() {
        return additional;
    }
}
