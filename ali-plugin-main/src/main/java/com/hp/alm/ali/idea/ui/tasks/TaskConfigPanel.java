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

package com.hp.alm.ali.idea.ui.tasks;

import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.tasks.TaskConfig;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.ui.chooser.EntityChooser;
import com.hp.alm.ali.idea.ui.chooser.HierarchicalChooser;
import com.hp.alm.ali.idea.services.FavoritesService;
import com.hp.alm.ali.idea.rest.RestListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.entity.tree.FavoritesModel;
import com.hp.alm.ali.idea.ui.entity.query.EntityQueryPicker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class TaskConfigPanel extends JPanel implements RestListener {

    private FavoritesService favoritesService;
    private String entityType;
    private TaskConfig config;

    private JComboBox queryCombo;

    public TaskConfigPanel(final Project project, String title, final TaskConfig config, String entityType, final ItemListener itemListener) {
        super(new GridBagLayout());

        this.favoritesService = project.getComponent(FavoritesService.class);
        this.entityType = entityType;
        this.config = config;

        project.getComponent(RestService.class).addListener(this);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        final JCheckBox mainCheck = new JCheckBox(title, config.isEnabled());
        add(mainCheck, c);
        c.gridy++;
        c.insets = new Insets(0, 10, 0, 0);
        final JRadioButton customQuery = new JRadioButton("Use Custom Query", config.isCustomSelected());
        customQuery.setEnabled(mainCheck.isSelected());
        add(customQuery, c);
        c.gridy++;
        c.insets = new Insets(0, 32, 0, 0);
        // config.customFilter doesn't have entityType set
        EntityQuery entityQuery = new EntityQuery(entityType);
        entityQuery.copyFrom(config.getCustomFilter());
        final EntityQueryPicker queryField = new EntityQueryPicker(project, entityQuery, entityType);
        queryField.addListener(new FilterListener<EntityQuery>() {
            @Override
            public void filterChanged(EntityQuery query) {
                config.getCustomFilter().copyFrom(query);
            }
        });
        queryField.setEnabled(mainCheck.isSelected() && config.isCustomSelected());
        add(queryField, c);
        c.gridy++;

        c.insets = new Insets(0, 10, 0, 0);
        final JRadioButton storedQuery = new JRadioButton("Use Stored Query", !config.isCustomSelected());
        storedQuery.setEnabled(mainCheck.isSelected());
        add(storedQuery, c);
        c.gridy++;
        c.insets = new Insets(0, 32, 0, 0);
        queryCombo = new JComboBox();
        reloadFavorites();
        if(!"".equals(config.getStoredQuery())) {
            // try to reselect stored value
            select(queryCombo, new ComboItem(config.getStoredQuery()));
        }
        enableQueryCombo(mainCheck.isSelected() && storedQuery.isSelected());
        add(queryCombo, c);
        c.gridy++;

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        add(new JPanel(), c);

        ButtonGroup group = new ButtonGroup();
        group.add(customQuery);
        group.add(storedQuery);

        mainCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                boolean selected = mainCheck.isSelected();
                config.setEnabled(selected);

                customQuery.setEnabled(selected);
                queryField.setEnabled(selected && customQuery.isSelected());
                storedQuery.setEnabled(selected);
                enableQueryCombo(selected && storedQuery.isSelected());

                if(selected && !storedQuery.isSelected()) {
                    customQuery.setSelected(true);
                }

                itemListener.itemStateChanged(itemEvent);
            }
        });

        customQuery.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                boolean selected = customQuery.isSelected();
                config.setCustomSelected(selected);

                queryField.setEnabled(selected);
            }
        });

        storedQuery.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                boolean selected = storedQuery.isSelected();
                config.setCustomSelected(!selected);

                enableQueryCombo(selected);
            }
        });

        queryCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    if(itemEvent.getItem() instanceof ServerFavorites) {
                        RestService restService = project.getComponent(RestService.class);
                        if(!restService.getServerTypeIfAvailable().isConnected()) {
                            Messages.showDialog("Not connected to HP ALM, server favorites are not available.", "Not Connected", new String[]{"Continue"}, 0, Messages.getErrorIcon());
                            revert();
                            return;
                        }
                        FavoritesModel favoritesModel = new FavoritesModel(project, TaskConfigPanel.this.entityType);
                        EntityChooser popup = new HierarchicalChooser(project, "favorite", true, false, true, false, favoritesModel);
                        popup.setVisible(true);
                        String selectedValue = popup.getSelectedValue();
                        if(!selectedValue.isEmpty()) {
                            Entity favorite = favoritesModel.getEntityNode(Integer.valueOf(selectedValue), "favorite").getEntity();
                            String favoriteName = favorite.getId() + ": " + favorite.getPropertyValue("name") + " (ALM)";
                            config.setStoredQuery(favoriteName);
                            ComboItem comboItem = new ComboItem(favoriteName);
                            queryCombo.setSelectedItem(comboItem);
                            if(!comboItem.equals(queryCombo.getSelectedItem())) {
                                queryCombo.addItem(comboItem);
                                queryCombo.setSelectedIndex(queryCombo.getItemCount() - 1);
                            }
                        } else {
                            revert();
                        }
                    } else {
                        config.setStoredQuery(((ComboItem)itemEvent.getItem()).getDisplayValue());
                    }
                }
            }
        });
    }

    private void revert() {
        // return to previous choice
        for(int i = 0; i < queryCombo.getItemCount(); i++) {
            ComboItem item = (ComboItem) queryCombo.getItemAt(i);
            if(item.getDisplayValue().equals(config.getStoredQuery())) {
                queryCombo.setSelectedItem(item);
                break;
            }
        }
    }

    private void enableQueryCombo(boolean enable) {
        queryCombo.setEnabled(enable);
        if(enable) {
            ComboItem item = (ComboItem)queryCombo.getSelectedItem();
            // store selected value into config
            if(item != null) {
                config.setStoredQuery(item.getDisplayValue());
            }
        }
    }

    private void reloadFavorites() {
        List<EntityQuery> list = favoritesService.getAvailableQueries(entityType);
        ComboItem selectedItem = (ComboItem)queryCombo.getSelectedItem();
        queryCombo.removeAllItems();
        for(EntityQuery filter: list) {
            queryCombo.addItem(new ComboItem(filter.getName()));
        }
        addServerItem();
        if(selectedItem != null) {
            select(queryCombo, selectedItem);
        }
    }

    private void select(JComboBox comboBox, ComboItem item) {
        comboBox.setSelectedItem(item);
        if(!item.equals(comboBox.getSelectedItem())) {
            if(item.getDisplayValue().endsWith("(ALM)")) {
                comboBox.addItem(item);
            } else {
                comboBox.addItem(new NotFound(item.getDisplayValue()));
            }
            comboBox.setSelectedIndex(comboBox.getItemCount() - 1);
            removeEmptyItem(comboBox);
        }
    }

    private void addServerItem() {
        if(queryCombo.getItemCount() == 0) {
            // server favorites triggers popup, it must not be selected, insert dummy item
            queryCombo.addItem(new ComboItem(""));
        }
        queryCombo.addItem(new ServerFavorites());
    }

    private void removeEmptyItem(JComboBox comboBox) {
        for(int i = 0; i < comboBox.getItemCount(); i++) {
            if(comboBox.getItemAt(i).toString().isEmpty()) {
                comboBox.removeItemAt(i);
            }
        }
    }

    public void restConfigurationChanged() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                reloadFavorites();
            }
        });
    }

    private static class NotFound extends ComboItem {
        NotFound(String value) {
            super(value);
        }

        public String toString() {
            return "Not found: " + super.toString();
        }
    }

    private static class ServerFavorites extends ComboItem {
        ServerFavorites() {
            super("Server Favorites...");
        }
    }
}
