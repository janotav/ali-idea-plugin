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
import com.hp.alm.ali.idea.entity.EntityFilterTableModel;
import com.hp.alm.ali.idea.entity.FilterModelImpl;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.impl.IdeGlassPaneImpl;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.ui.WrapLayout;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import net.coderazzi.filters.gui.TableFilterHeader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class EntityFilterDialog<E extends EntityFilter<E>> extends MyDialog {
    protected EntityFilterModel<E> queryModel;
    protected JBTabsImpl tabs;
    private E savedQuery;
    protected E queryCopy;
    protected String entityType;
    protected Set<String> hiddenFields;
    protected MetadataService metadataService;
    protected FilterManager fieldManager;
    protected EntityLabelService entityLabelService;
    protected Project project;

    public EntityFilterDialog(final Project project, final String entityType, E query, final String title, Set<String> hiddenFields) {
        super(project, title, true);
        this.project = project;
        this.entityType = entityType;
        this.hiddenFields = hiddenFields;

        entityLabelService = project.getComponent(EntityLabelService.class);
        metadataService = project.getComponent(MetadataService.class);
        fieldManager = project.getComponent(FilterManager.class);
        entityLabelService.loadEntityLabelAsync(entityType, new AbstractCachingService.DispatchCallback<String>() {
            @Override
            public void loaded(String entityLabel) {
                setTitle(title + entityLabel);
            }
        });

        // Try to work around this:
        //   [  41196]  ERROR - pplication.impl.LaterInvocator - Glass pane should be com.intellij.openapi.wm.IdeGlassPane
        //   java.lang.IllegalArgumentException: Glass pane should be com.intellij.openapi.wm.IdeGlassPane
        Component ideGlassPane = IdeGlassPaneImpl.newInstance(getRootPane());
        if(ideGlassPane != null) {
            setGlassPane(ideGlassPane);
        }

        queryCopy = query.clone();

        tabs = new JBTabsImpl(project);
        Metadata metadata = metadataService.getEntityMetadata(entityType);
        tabs.addTab(new TabInfo(createFilterTable(queryCopy, metadata)).setText("Filter"));

        queryModel = new FilterModelImpl<E>(queryCopy);
        EntityFilterPanel entityFilterPanel = createFilterPanel();
        entityFilterPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 2, 0));
        JPanel np = new JPanel(new BorderLayout());
        np.add(entityFilterPanel, BorderLayout.CENTER);
        np.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(np, BorderLayout.NORTH);
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(tabs, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EntityFilterDialog.this.savedQuery = queryCopy;
                close(true);
            }
        });
        buttons.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                close(false);
            }
        });
        buttons.add(cancel);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(600, 400));
        setResizable(true);
        centerOnOwner();
    }

    protected void setCrossFilterTitleBorder(JPanel panel, String label, String entity) {
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK), label == null? entity: label));
    }

    protected EntityFilterPanel<E> createFilterPanel() {
        return new EntityFilterPanel<E>(project, queryModel, entityType, ";", true, true, false);
    }

    public E chooseQuery() {
        setVisible(true);
        return savedQuery;
    }

    protected JPanel createFilterTable(final E query, final Metadata metadata) {
        final EntityFilterTableModel<E> model = new EntityFilterTableModel<E>(project, metadata, query, hiddenFields);

        JBTable table = new JBTable() {
            public TableCellEditor getCellEditor(int row, int col) {
                int modelRow = convertRowIndexToModel(row);
                Field field = model.getFields().get(modelRow);
                FilterFactory factory = fieldManager.getFilterFactory(new Context<E>(query), entityType, field, true);
                if(factory != null) {
                    if(factory.getCustomChoices() == null) {
                        return new ReferenceCellEditor(factory, model);
                    } else {
                        return new LookupCellEditor(factory.getCustomChoices(), false);
                    }
                } else if(field.getListId() != null) {
                    List<String> list = project.getComponent(ProjectListService.class).getProjectList(entityType, field);
                    return new LookupCellEditor(list, true);
                } else {
                    return super.getCellEditor(row, col);
                }
            }
        };
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(true);
        table.setModel(model);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
        table.setRowSorter(sorter);
        new TableFilterHeader(table);
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent tableModelEvent) {
                queryModel.fireFilterUpdated(true);
            }
        });
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
