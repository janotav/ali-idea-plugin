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

import com.hp.alm.ali.idea.entity.EntityFilter;
import com.hp.alm.ali.idea.entity.EntityFilterModel;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.ui.FieldNameLabel;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.services.MetadataService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class EntityFilterPanel<E extends EntityFilter<E>> extends JPanel implements FilterListener<E>, ServerTypeListener {
    protected JLabel emptyLabel;
    protected JLabel filterLabel;
    private JLabel endOfFilterLabel;

    private Map<String, WherePanel> propertyMap = new HashMap<String, WherePanel>();

    private TranslateService translateService;
    private MetadataService metadataService;
    protected EntityLabelService entityLabelService;
    private Project project;
    private EntityFilterModel<E> model;
    private String entityType;
    protected String delimiter;
    protected boolean showSection;
    protected boolean quickRemove;

    public EntityFilterPanel(final Project project, final EntityFilterModel<E> model, final String entityType, String delimiter, boolean showEmptyLabel, boolean showSection, boolean quickRemove) {
        this(project, model, entityType, delimiter, showEmptyLabel, showSection, quickRemove, true);
    }

    public EntityFilterPanel(final Project project, final EntityFilterModel<E> model, final String entityType, String delimiter, boolean showEmptyLabel, boolean showSection, boolean quickRemove, boolean loadMetadata) {
            this.project = project;
        this.model = model;
        this.entityType = entityType;
        this.delimiter = delimiter == null? "": delimiter;
        this.showSection = showSection;
        this.quickRemove = quickRemove;
        this.metadataService = project.getComponent(MetadataService.class);
        this.entityLabelService = project.getComponent(EntityLabelService.class);
        this.translateService = project.getComponent(TranslateService.class);

        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        setBorder(BorderFactory.createEmptyBorder());

        setVisible(false);

        project.getComponent(RestService.class).addServerTypeListener(this);

        if(showEmptyLabel) {
            emptyLabel = new JLabel("<no filter defined>");
            emptyLabel.setVisible(false);
            add(emptyLabel);
        }

        filterLabel = makeLabel("Filter:");
        filterLabel.setVisible(false);
        add(filterLabel);

        endOfFilterLabel = new JLabel();
        endOfFilterLabel.setVisible(false);
        add(endOfFilterLabel);

        if(loadMetadata) {
            loadMetadata();
        }

        model.addFilterListener(this);
    }

    protected void loadMetadata() {
        metadataService.loadEntityMetadataAsync(entityType, new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(Metadata metadata) {
                setVisible(true);
                update(model.getFilter(), true);
            }

            @Override
            public void metadataFailed() {
                setVisible(false);
            }
        });
    }

    protected JLabel makeLabel(String name) {
        JLabel jLabel = new JLabel(name);
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        return jLabel;
    }

    protected void removeObsoletePanels(Map<String, ? extends JComponent> map, Set<String> filter) {
        for(Iterator<String> it = map.keySet().iterator(); it.hasNext(); ) {
            String property = it.next();
            if(!filter.contains(property)) {
                remove(map.get(property));
                it.remove();
            }
        }
    }

    protected void addNextTo(Component newComp, Component comp, int ofs) {
        LinkedList<Component> components = new LinkedList<Component>(Arrays.asList(getComponents()));
        int i = components.indexOf(comp);
        add(newComp, i + ofs);
    }

    protected <T, E extends ColumnPanel<T>, X> void updateProperties(ColumnPanelFactory<T, E, X> factory, String entityType, Map<String, T> filter, X query, Map<String, E> propertyMap, JComponent anchor, int ofs) {
        for(String property: filter.keySet()) {
            T value = filter.get(property);
            E columnPanel = propertyMap.get(property);
            if(columnPanel == null) {
                columnPanel = factory.create(entityType, query, property, value);
                propertyMap.put(property, columnPanel);
                if(anchor == null) {
                    add(columnPanel);
                } else {
                    addNextTo(columnPanel, anchor, ofs);
                }
            } else {
                columnPanel.setValue(value);
            }
        }
    }

    protected void removeObsoletePanels(E query) {
        removeObsoletePanels(propertyMap, query.getPropertyMap().keySet());
    }

    protected void update(E query, boolean force) {
        if(force) {
            // passing empty query causes all panels to be cleared
            E clone = query.clone();
            clone.clear();
            removeObsoletePanels(clone);
        } else {
            removeObsoletePanels(query);
        }

        updateProperties(new WherePanelFactory(), query.getEntityType(), query.getPropertyMap(), query, propertyMap, endOfFilterLabel, 0);

        if(showSection) {
            filterLabel.setVisible(!query.getPropertyMap().isEmpty());
        }

        if(emptyLabel != null)  {
            emptyLabel.setVisible(query.isEmpty());
        }
    }

    public void filterChanged(E query) {
        update(query, false);
        revalidate();
        repaint();
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            loadMetadata();
        }
    }

    private class RemoveFilter implements LinkListener {
        private EntityFilter filter;
        private final String property;

        public RemoveFilter(EntityFilter filter, String property) {
            this.filter = filter;
            this.property = property;
        }

        public void linkSelected(LinkLabel linkLabel, Object o) {
            filter.setValue(property, null);
            refresh();
        }
    }

    protected void refresh() {
        revalidate();
        repaint();

        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                model.fireFilterUpdated(true);
            }
        });
    }

    protected static abstract class ColumnPanel<T> extends JPanel {
        protected JLabel valueLabel;
        protected String entityType;
        protected String property;
        private FieldNameLabel nameLabel;

        public ColumnPanel(final EntityFilterPanel parent, String entityType, String property, T value, LinkListener listener) {
            this.entityType = entityType;
            this.property = property;
            nameLabel = new FieldNameLabel(entityType, property, parent.project.getComponent(MetadataService.class)) {
                @Override
                public Color getForeground() {
                    return parent.isEnabled() ? SystemColor.textText : SystemColor.textInactiveText;
                }
            };
            valueLabel = new JLabel() {
                @Override
                public Color getForeground() {
                    return parent.isEnabled() ? SystemColor.textText : SystemColor.textInactiveText;
                }
            };
            setValue(value);

            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

            add(nameLabel);
            add(valueLabel);

            if(parent.quickRemove) {
                add(new LinkLabel("", IconLoader.getIcon("/actions/clean.png"), listener, this));
            }
        }

        protected abstract void setValue(T value);

    }

    protected interface ColumnPanelFactory<T, E extends ColumnPanel<T>, X> {

        E create(String entityType, X filter, String property, T value);

    }

    protected class WherePanel extends ColumnPanel<String> {

        public WherePanel(String entityType, String prop, String value, LinkListener listener) {
            super(EntityFilterPanel.this, entityType, prop, value, listener);
        }

        protected void setValue(final String value) {
            metadataService.loadEntityMetadataAsync(entityType, new MetadataService.MetadataCallback() {
                @Override
                public void metadataLoaded(Metadata metadata) {
                    Field field = metadata.getField(property);
                    translateService.convertQueryModelToView(field, value, new ValueCallback.Dispatch(new ValueCallback() {
                        @Override
                        public void value(String value) {
                            valueLabel.setText("[" + value + "]" + delimiter);
                        }
                    }));
                }

                @Override
                public void metadataFailed() {
                    valueLabel.setText("[" + value + "]" + delimiter);
                }
            });
        }
    }

    protected class WherePanelFactory implements ColumnPanelFactory<String, WherePanel, EntityFilter> {

        @Override
        public WherePanel create(String entityType, EntityFilter filter, String property, String value) {
            return new WherePanel(entityType, property, value, new RemoveFilter(filter, property));
        }
    }
}
