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

package com.hp.alm.ali.idea.content.detail;

import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.entity.table.QuerySharingManager;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.LinkedList;
import java.util.List;

public class TableContent extends JPanel implements DetailContent, TableModelListener, DataProvider {

    private EntityTable entityTable;
    private Entity entity;
    private final String label;
    private final Icon icon;
    private JPanel toolbars;
    private ActionToolbar entityToolbar;
    private EntityTableLoader loader;
    private QuerySharingManager querySharingManager;
    final private List<ChangeListener> listeners;

    public TableContent(Project project, Entity entity, String label, Icon icon, ActionToolbar toolbar, EntityTableLoader loader) {
        super(new BorderLayout());

        this.entity = entity;
        this.label = label;
        this.icon = icon;
        this.loader = loader;
        this.listeners = new LinkedList<ChangeListener>();
        this.querySharingManager = project.getComponent(QuerySharingManager.class);

        toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        if(toolbar != null) {
            toolbar.setTargetComponent(this);
            toolbars.add(toolbar.getComponent());
        }
        add(toolbars, BorderLayout.NORTH);
        entityToolbar = ActionUtil.createActionToolbar("hpali.entity", "detail-table", true);
    }

    public void remove() {
        removeExistingTable();
        synchronized (listeners) {
            listeners.clear();
        }
    }

    private void removeExistingTable() {
        if(entityTable != null) {
            toolbars.remove(entityToolbar.getComponent());
            EntityTableModel model = entityTable.getModel();
            model.removeTableModelListener(this);
            querySharingManager.removeSharedQuery(model, model.getFilter().getEntityType());
        }
    }

    public void setTable(EntityTable entityTable) {
        if(this.entityTable == entityTable) {
            if(entityTable != null) {
                entityTable.getModel().reload();
            }
            return;
        }
        removeExistingTable();
        this.entityTable = entityTable;
        if(entityTable != null) {
            entityToolbar.setTargetComponent(entityTable);
            toolbars.add(entityToolbar.getComponent());
            add(entityTable, BorderLayout.CENTER);
            EntityTableModel model = entityTable.getModel();
            model.addTableModelListener(this);
            querySharingManager.addSharedQuery(model, model.getFilter().getEntityType());
        }
        fireChangeEvent(this);
    }

    private void fireChangeEvent(Object source) {
        synchronized (listeners) {
            for(ChangeListener listener: listeners) {
                listener.stateChanged(new ChangeEvent(source));
            }
        }
    }

    @Override
    public void reload() {
        loader.load(this);
    }

    @Override
    public Component getComponent() {
        if(entityTable == null) {
            return null;
        } else {
            return this;
        }
    }

    @Override
    public String getLinkText() {
        if(entityTable == null) {
            return label;
        } else {
            return label + " (" + entityTable.getModel().getRowCount() + ")";
        }
    }

    @Override
    public void addChangeListener(final ChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public Dimension getPreferredSize() {
        if(entityTable == null) {
            return super.getPreferredSize();
        }
        Dimension dim = entityTable.getPreferredSize();
        int rowCount = entityTable.getModel().getRowCount();
        dim.height = rowCount * entityTable.getTable().getRowHeight() + 32; // row content + header
        if(toolbars != null) {
            dim.height += toolbars.getPreferredSize().height;
        }
        if(rowCount == 0) {
            dim.height += 18; // Nothing to show label
        }
        return dim;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        fireChangeEvent(e);
    }

    @Override
    public Object getData(@NonNls String s) {
//        if("entity-list".equals(s)) {
//            return Arrays.asList(entity);
//        }
        return null;
    }

    public static interface EntityTableLoader {

        void load(TableContent content);

    }
}
