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

package com.hp.alm.ali.idea.ui.entity.table;

import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.ui.entity.query.QuerySaveAsDialog;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.ui.chooser.PopupDialog;
import com.hp.alm.ali.idea.entity.tree.FavoritesModel;
import com.hp.alm.ali.idea.ui.MultipleItemsDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ColumnHeaderPopup extends JPopupMenu {

    private String entityType;

    public ColumnHeaderPopup(final Project project, final JTable table, final EntityTableModel model, boolean useFavorites) {
        this.entityType = model.getFilter().getEntityType();

        final AliConfiguration projConf = project.getComponent(AliProjectConfiguration.class);
        final AliConfiguration conf = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
        TranslateService translateService = project.getComponent(TranslateService.class);

        final List<Field> available = new ArrayList<Field>(model.getFields());
        for(Iterator<Field> it = available.iterator(); it.hasNext(); ) {
            Field field = it.next();
            if(field.isBlob() && !translateService.isTranslated(field)) {
                it.remove();
                continue;
            }
            if(model.getHiddenFields().contains(field.getName())) {
                it.remove();
            }
        }
        Collections.sort(available, Field.LABEL_COMPARATOR);
        final List<String> visibleColumns = new ArrayList<String>();
        final TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            final TableColumn column = columnModel.getColumn(i);
            visibleColumns.add(model.getFields().get(column.getModelIndex()).getName());
        }
        JMenuItem more = new JMenuItem("More Columns...");
        more.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                MultipleItemsDialog dialog = new MultipleItemsDialog(project, "Column", true, available, visibleColumns);
                dialog.setVisible(true);
                if(dialog.isOk()) {
                    EntityTableModel model = (EntityTableModel) table.getModel();
                    EntityQuery filter = model.getFilter();
                    LinkedHashMap<String, Integer> columns = filter.getColumns();
                    columns.keySet().retainAll(visibleColumns);
                    for (String field : visibleColumns) {
                        if (!columns.containsKey(field)) {
                            columns.put(field, 75);
                        }
                    }
                    model.setColumns(columns);
                }
            }
        });
        add(more);
        add(new JSeparator());

        if(useFavorites) {
            JMenu stored = new JMenu("Stored Queries");
            add(stored);

            JMenuItem alm = new JMenuItem("Server Favorites");
            alm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    FavoritesModel favoritesModel = new FavoritesModel(project, entityType);
                    PopupDialog popup = new PopupDialog(project, "favorite", true, false, PopupDialog.Selection.FOLLOW_ID, false, favoritesModel);
                    popup.setVisible(true);
                    String selectedValue = popup.getSelectedValue();
                    if(!selectedValue.isEmpty()) {
                        EntityQuery query = (EntityQuery)favoritesModel.getEntityNode(Integer.valueOf(selectedValue), "favorite").getEntity().getProperty("query");
                        model.setFilter(query);
                    }
                }
            });
            stored.add(alm);
            stored.add(new JSeparator());

            final List<EntityQuery> storedFilters = projConf.getStoredFilters(entityType);
            for(EntityQuery f: sort(storedFilters)) {
                stored.add(addItem(f, "project", model));
            }
            final List<EntityQuery> globalFilters = conf.getStoredFilters(entityType);
            for(EntityQuery f: sort(globalFilters)) {
                stored.add(addItem(f, "global", model));
            }

            JMenuItem savePrj = new JMenuItem("Store Current (in project)");
            savePrj.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    storeQuery(projConf, model.getFilter(), storedFilters);
                }
            });
            add(savePrj);

            JMenuItem saveGlobal = new JMenuItem("Store Current (global)");
            saveGlobal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    storeQuery(conf, model.getFilter(), globalFilters);
                }
            });
            add(saveGlobal);

            if(!storedFilters.isEmpty() || !globalFilters.isEmpty()) {
                final JMenuItem  manage = new JMenuItem("Manage Queries");
                manage.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        List<String> list = new LinkedList<String>();
                        for(EntityQuery f: storedFilters) {
                            list.add(f.getName() + " (project)");
                        }
                        for(EntityQuery f: globalFilters) {
                            list.add(f.getName() + " (global)");
                        }
                        String s = (String) JOptionPane.showInputDialog(
                                manage,
                                "Drop Stored Query:\n",
                                "Manage Stored Queries",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                list.toArray(),
                                null);
                        if(s != null) {
                            int i = list.indexOf(s);
                            if(i < storedFilters.size()) {
                                projConf.dropFilter(entityType, storedFilters.get(i).getName());
                            } else {
                                conf.dropFilter(entityType, globalFilters.get(i - storedFilters.size()).getName());
                            }
                        }
                    }
                });
                add(manage);
            }

            add(new JSeparator());
        }

        // visible columns

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            final TableColumn column = columnModel.getColumn(i);
            Field field = model.getFields().get(column.getModelIndex());
            String columnName = field.getLabel();
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(columnName, true);
            if(table.getColumnModel().getColumnCount() == 1) {
                // don't allow to remove last column
                menuItem.setEnabled(false);
            } else {
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        columnModel.removeColumn(column);
                        model.columnRemovedFromView(column.getModelIndex());
                    }
                });
            }
            add(menuItem);
        }
    }

    private void storeQuery(AliConfiguration conf, EntityQuery filter, List<EntityQuery> storedFilters) {
        List<String> names = new LinkedList<String>();
        for(EntityQuery f: storedFilters) {
            names.add(f.getName());
        }
        QuerySaveAsDialog saveAs = new QuerySaveAsDialog(names);
        String name = saveAs.getTargetName();
        if(name != null) {
            EntityQuery clone = filter.clone();
            clone.setName(name);
            conf.storeFilter(entityType, clone);
        }
    }

    private List<EntityQuery> sort(List<EntityQuery> list) {
        ArrayList<EntityQuery> ret = new ArrayList<EntityQuery>(list);
        Collections.sort(ret, EntityQuery.COMPARATOR_NAME);
        return ret;
    }

    private JMenuItem addItem(final EntityQuery filter, String type, final EntityTableModel model) {
        JMenuItem menuItem = new JMenuItem(filter.getName() + " ("+type+")");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                model.setFilter(filter);
            }
        });
        return menuItem;
    }
}
