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
import com.hp.alm.ali.idea.ui.EntityLabel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SortOrder;
import java.awt.Font;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EntityQueryPanel extends EntityFilterPanel<EntityQuery> {
    private Icon modifyFilterIcon = IconLoader.getIcon("/debugger/class_filter.png");

    private JLabel crossLabel;
    private JLabel orderLabel;

    private Map<String, EntityLabel> crossMap = new HashMap<String, EntityLabel>();
    private Map<String, Map<String, WherePanel>> crossPropertyMap = new HashMap<String, Map<String, WherePanel>>();
    private Map<String, OrderPanel> orderMap = new HashMap<String, OrderPanel>();

    public EntityQueryPanel(final Project project, final EntityFilterModel<EntityQuery> model, final String entityType, final Set<String> hiddenFields, String delimiter, boolean showEmptyLabel, boolean showSection, boolean showIcon, boolean quickRemove) {
        super(project, model, entityType, delimiter, showEmptyLabel, showSection, quickRemove, false);

        crossLabel  = makeLabel("Cross-Filter:");
        crossLabel.setVisible(false);
        add(crossLabel);

        orderLabel = makeLabel("Order By:");
        orderLabel.setVisible(false);
        add(orderLabel);

        if(showIcon) {
            LinkLabel modifyFilterLink = new LinkLabel("", modifyFilterIcon, new LinkListener() {
                public void linkSelected(LinkLabel aSource, Object aLinkData) {
                    EntityQuery updatedQuery = new EntityQueryDialog(project, entityType, model.getFilter(), "Filter:", hiddenFields).chooseQuery();
                    if(updatedQuery != null) {
                        model.getFilter().copyFrom(updatedQuery);
                        model.fireFilterUpdated(true);
                    }
                }
            });
            add(modifyFilterLink, 0);
        }

        loadMetadata();
    }

    protected void removeObsoletePanels(EntityQuery query) {
        super.removeObsoletePanels(query);
        for(String alias: crossPropertyMap.keySet()) {
            removeObsoletePanels(crossPropertyMap.get(alias), query.getCrossFilter(alias).getPropertyMap().keySet());
        }
        removeObsoletePanels(orderMap, query.getOrder().keySet());
        for(Iterator<String> it = crossMap.keySet().iterator(); it.hasNext(); ) {
            String alias = it.next();
            if(query.getCrossFilter(alias).isEmpty()) {
                remove(crossMap.get(alias));
                it.remove();
            }
        }
    }

    protected void update(EntityQuery query, boolean force) {
        super.update(query, force);

        Map<String, EntityCrossFilter> crossFilters = query.getCrossFilters();
        for(String alias: crossFilters.keySet()) {
            EntityCrossFilter crossQuery = query.getCrossFilter(alias);
            EntityLabel entityLabel = crossMap.get(alias);
            String suffix = crossQuery.isInclusive() ? "" : "<Exclusive>";
            if(entityLabel == null) {
                entityLabel = new EntityLabel(alias, entityLabelService, suffix);
                entityLabel.setFont(entityLabel.getFont().deriveFont(Font.BOLD));
                crossMap.put(alias, entityLabel);
                addNextTo(entityLabel, orderLabel, 0); // cross filter groups go before order
            } else {
                entityLabel.setSuffix(suffix);
            }
            Map<String, WherePanel> map = crossPropertyMap.get(alias);
            if(map == null) {
                map = new HashMap<String, WherePanel>();
                crossPropertyMap.put(alias, map);
            }
            updateProperties(new WherePanelFactory(), crossQuery.getEntityType(), crossQuery.getPropertyMap(), crossQuery, map, entityLabel, 1);
        }

        updateProperties(new OrderPanelFactory(), query.getEntityType(), query.getOrder(), query, orderMap, null, 0);

        if(showSection) {
            crossLabel.setVisible(!crossFilters.isEmpty());
            orderLabel.setVisible(!query.getOrder().isEmpty());
        }
    }

    private class RemoveOrder implements LinkListener {
        private EntityQuery query;
        private String property;

        public RemoveOrder(EntityQuery query, String property) {
            this.query = query;
            this.property = property;
        }

        public void linkSelected(LinkLabel linkLabel, Object o) {
            query.removeOrder(property);
            refresh();
        }
    }

    private class OrderPanel extends ColumnPanel<SortOrder> {

        public OrderPanel(EntityFilterPanel parent, String entityType, String prop, SortOrder value, LinkListener listener) {
            super(parent, entityType, prop, value, listener);
        }

        @Override
        protected void setValue(SortOrder value) {
            valueLabel.setText("[" + (value == SortOrder.ASCENDING? "ASC": "DESC") + "]" + delimiter);
        }
    }

    private class OrderPanelFactory implements ColumnPanelFactory<SortOrder, OrderPanel, EntityQuery> {

        @Override
        public OrderPanel create(String entityType, EntityQuery query, String property, SortOrder value) {
            return new OrderPanel(EntityQueryPanel.this, entityType, property, value, new RemoveOrder(query, property));
        }
    }
}
