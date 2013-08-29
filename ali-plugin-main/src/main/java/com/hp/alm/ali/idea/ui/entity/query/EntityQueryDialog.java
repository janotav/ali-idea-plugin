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

package com.hp.alm.ali.idea.ui.entity.query;

import com.hp.alm.ali.idea.entity.EntityCrossFilter;
import com.hp.alm.ali.idea.entity.EntityFilterModel;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.entity.FilterModelImpl;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.model.Relation;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.TabInfo;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityQueryDialog extends EntityFilterDialog<EntityQuery> {

    public EntityQueryDialog(Project project, String entityType, EntityQuery query, String title, Set<String> hiddenFields) {
        super(project, entityType, query, title, hiddenFields);

        Metadata metadata = metadataService.getEntityMetadata(entityType);

        Map<String, List<Relation>> relationMap = metadata.getRelationMap(true);
        if(!relationMap.isEmpty()) {
            JPanel crossPanel = new JPanel();
            crossPanel.setLayout(new BoxLayout(crossPanel, BoxLayout.Y_AXIS));
            crossPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
            for(final String entity: relationMap.keySet()) {
                final JPanel crossEntity = new JPanel(new GridBagLayout());

                entityLabelService.loadEntityLabelAsync(entity, new AbstractCachingService.DispatchCallback<String>() {
                    @Override
                    public void loaded(String entityLabel) {
                        setCrossFilterTitleBorder(crossEntity, entityLabel, entity);
                    }
                });

                int row = 0;
                List<Relation> relations = relationMap.get(entity);
                for(Relation relation: relations) {
                    List<String> aliases = relation.getAliases();
                    if(!aliases.isEmpty()) {
                        addAlias(row++, relations.size() > 1, crossEntity, entity, aliases.get(0), queryCopy);
                    }
                }

                crossPanel.add(crossEntity);
            }
            JPanel up = new JPanel(new BorderLayout());
            up.add(crossPanel, BorderLayout.NORTH);
            tabs.addTab(new TabInfo(new JBScrollPane(up)).setText("Cross Filter"));
        }

        OrderPanel orderPanel = new OrderPanel(project, queryCopy, metadata);
        orderPanel.addQueryListener(new FilterListener<EntityQuery>() {
            public void filterChanged(EntityQuery query) {
                queryModel.fireFilterUpdated(true);
            }
        });
        tabs.addTab(new TabInfo(new JBScrollPane(orderPanel)).setText("Order"));

    }

    protected EntityQueryPanel createFilterPanel() {
        return new EntityQueryPanel(project, queryModel, entityType, hiddenFields, ";", true, true, false, false);
    }

    private void addAlias(int row, boolean withLabel, JPanel crossEntity, final String entity, final String alias, final EntityQuery query) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = row * 3;
        c.gridwidth = 3;
        if(withLabel) {
            crossEntity.add(new JLabel(alias + ":"), c);
        }
        c.gridy++;
        ButtonGroup group = new ButtonGroup();
        final JRadioButton none = new JRadioButton("None");
        crossEntity.add(none, c);
        group.add(none);
        c.gridwidth = 1;
        c.gridy++;
        JRadioButton crossFilter = new JRadioButton("");
        crossEntity.add(crossFilter, c);
        group.add(crossFilter);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        final EntityFilterModel<EntityCrossFilter> crossQueryModel = new FilterModelImpl<EntityCrossFilter>(query.getCrossFilter(entity, alias));
        final EntityFilterPanel crossEntityFilterPanel = new EntityFilterPanel<EntityCrossFilter>(project, crossQueryModel, entity, ";", true, false, false, true);
        crossEntityFilterPanel.setBorder(BorderFactory.createEtchedBorder());
        crossEntity.add(crossEntityFilterPanel, c);
        final EntityCrossFilter cf = query.getCrossFilter(entity, alias).clone();
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        final JButton browse = new JButton("...");
        browse.setPreferredSize(new Dimension(25, 18));
        crossEntity.add(browse, c);

        if(cf.getPropertyMap().isEmpty()) {
            crossEntityFilterPanel.setEnabled(false);
            browse.setEnabled(false);
            none.setSelected(true);
        } else {
            crossEntityFilterPanel.setEnabled(true);
            browse.setEnabled(true);
            crossFilter.setSelected(true);
        }
        none.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    crossEntityFilterPanel.setEnabled(false);
                    browse.setEnabled(false);
                    // remember cross filter in case we will reselect it later
                    cf.copyFrom(query.getCrossFilter(entity, alias));
                    // but clear it from the main filter immediately
                    query.getCrossFilter(entity, alias).clear();
                    queryModel.fireFilterUpdated(true);
                }
            }
        });
        crossFilter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    crossEntityFilterPanel.setEnabled(true);
                    browse.setEnabled(true);
                    if(cf.getPropertyMap().isEmpty()) {
                        // ask for new values
                        if(!showCrossFilterDialog(entity, alias, cf, query, crossQueryModel)) {
                            none.setSelected(true);
                        }
                    } else {
                        // restore values
                        query.getCrossFilter(entity, alias).copyFrom(cf);
                        queryModel.fireFilterUpdated(true);
                    }
                }
            }
        });
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if(!showCrossFilterDialog(entity, alias, query.getCrossFilter(entity, alias), query, crossQueryModel)) {
                    none.setSelected(true);
                }
            }
        });
    }

    private boolean showCrossFilterDialog(String crossEntity, String alias, EntityCrossFilter crossFilter, EntityQuery query, EntityFilterModel crossQueryModel) {
        EntityFilterDialog<EntityCrossFilter> crossQueryDialog = new EntityCrossFilterDialog(project, crossEntity, crossFilter, "CrossFilter: ", Collections.<String>emptySet());
        Point loc = getLocation();
        loc.translate(50, 50);
        crossQueryDialog.setLocation(loc);
        EntityCrossFilter updatedQuery = crossQueryDialog.chooseQuery();
        if(updatedQuery != null) {
            query.getCrossFilter(crossEntity, alias).copyFrom(updatedQuery);
            crossQueryModel.fireFilterUpdated(true);
            queryModel.fireFilterUpdated(true);
            return !updatedQuery.getPropertyMap().isEmpty();
        } else {
            return !crossFilter.getPropertyMap().isEmpty();
        }
    }
}
