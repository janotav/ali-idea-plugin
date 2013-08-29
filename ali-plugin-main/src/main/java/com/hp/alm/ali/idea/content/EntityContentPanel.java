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

package com.hp.alm.ali.idea.content;

import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.ui.ActiveItemLink;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Set;

public class EntityContentPanel extends JPanel {

    private Project project;
    private EntityTable entityTable;
    private String entityType;
    private EntityAction entityAction;

    public EntityContentPanel(Project project, String entityType, Component toolbar) {
        this(project, entityType, toolbar, Collections.<String>emptySet(), null);
    }

    public EntityContentPanel(final Project project, final String entityType, Component toolbar, Set<String> hiddenFields, EntityQueryProcessor processor) {
        super(new BorderLayout());

        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);

        this.project = project;
        this.entityType = entityType;
        this.entityAction = new DefaultEntityAction();

        entityTable = new EntityTable(project, entityType, true, conf.getFilter(entityType), hiddenFields, processor, true, null);

        JPanel filterAndToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterAndToolbar.setBorder(BorderFactory.createEtchedBorder());
        filterAndToolbar.add(entityTable.getQueryPanel());
        filterAndToolbar.add(toolbar);
        add(new JBScrollPane(filterAndToolbar, JBScrollPane.VERTICAL_SCROLLBAR_NEVER, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.NORTH);

        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.entity", "table", false, false);
        actionToolbar.setTargetComponent(entityTable);
        add(actionToolbar.getComponent(), BorderLayout.WEST);

        JPanel statusBar = new JPanel();
        statusBar.setBackground(Color.WHITE);
        statusBar.setLayout(new BorderLayout());
        statusBar.add(entityTable.getStatusComponent(), BorderLayout.WEST);
        statusBar.add(new ActiveItemLink(project), BorderLayout.EAST);
        JPanel tableAndStatusBar = new JPanel(new BorderLayout());
        tableAndStatusBar.add(entityTable, BorderLayout.CENTER);
        tableAndStatusBar.add(statusBar, BorderLayout.SOUTH);
        add(tableAndStatusBar, BorderLayout.CENTER);

        entityTable.getTable().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int idx = entityTable.getTable().convertRowIndexToModel(entityTable.getTable().getSelectedRow());
                    entityAction.perform(entityTable.getModel().getEntity(idx));
                }
            }
        });
    }

    public void scrollTo(Entity entity) {
        int row = entityTable.getModel().indexOf(entity);
        if(row >= 0) {
            int viewRow = entityTable.getTable().convertRowIndexToView(row);
            entityTable.getTable().setRowSelectionInterval(viewRow, viewRow);
            entityTable.getTable().scrollRectToVisible(new Rectangle(entityTable.getTable().getCellRect(viewRow, 0, true)));
        }
    }

    public void goTo(String idStr) {
        int id;
        try {
            id = Integer.valueOf(idStr);
        } catch(NumberFormatException e) {
            return;
        }
        Entity entity = new Entity(entityType, id);
        AliContentFactory.loadDetail(project, entity, true, true);
        entityTable.scrollTo(entity);
    }

    public EntityTable getEntityTable() {
        return entityTable;
    }

    public void setEntityAction(EntityAction entityAction) {
        this.entityAction = entityAction;
    }

    public EntityAction getEntityAction() {
        return entityAction;
    }

    private class DefaultEntityAction implements EntityAction {
        @Override
        public void perform(Entity entity) {
            AliContentFactory.loadDetail(project, entity, true, true);
            scrollTo(entity);
        }
    }

    public static interface EntityAction {

        void perform(Entity entity);

    }
}
