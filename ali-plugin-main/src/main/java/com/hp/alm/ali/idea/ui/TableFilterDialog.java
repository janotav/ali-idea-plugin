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

import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.hp.alm.ali.idea.model.ItemRenderer;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import net.coderazzi.filters.gui.IFilterEditor;
import net.coderazzi.filters.gui.IFilterHeaderObserver;
import net.coderazzi.filters.gui.TableFilterHeader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableFilterDialog<E> extends MyDialog implements IFilterHeaderObserver, ListSelectionListener {

    private ItemsProvider<E> provider;
    private ItemRenderer<E> renderer;
    private String column;
    private boolean ok;
    private JPanel content;
    private JBTable table;
    private JLabel loading;
    private JLabel tooMany;
    private JButton okButton;
    private ListTableModel model;
    private TableFilterHeader header;
    private List<E> items = new ArrayList<E>();
    private List<E> selectedItems = new ArrayList<E>();

    public TableFilterDialog(Project project, String title, boolean allowMultiple, String column, ItemsProvider<E> provider, ItemRenderer<E> renderer) {
        super(project, title, true);

        this.provider = provider;
        this.renderer = renderer;
        this.column = column;

        content = new JPanel(new BorderLayout());
        tooMany = new JLabel("Too many results, narrow your search");
        loading = new JLabel("Loading...", JLabel.CENTER);
        content.add(loading, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ok = true;
                for(int row: table.getSelectedRows()) {
                    selectedItems.add(items.get(table.convertRowIndexToModel(row)));
                }
                close(true);
            }
        });
        okButton.setEnabled(false);
        buttons.add(okButton);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                close(false);
            }
        });
        buttons.add(cancel);
        content.add(buttons, BorderLayout.SOUTH);
        getContentPane().add(content, BorderLayout.CENTER);

        // table only prepared (waiting for data)
        table = new JBTable();
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(true);
        table.setAutoCreateColumnsFromModel(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        model = new ListTableModel();
        table.setModel(model);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        table.setRowSorter(sorter);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        header = new TableFilterHeader(table);

        if(allowMultiple) {
            table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        table.getSelectionModel().addListSelectionListener(this);

        setPreferredSize(new Dimension(600, 400));
        setSize(getPreferredSize());
        setResizable(true);
        centerOnOwner();

        executeQuery(true, null);

    }

    public boolean isOk() {
        return ok;
    }

    public List<E> getSelectedItems() {
        return selectedItems;
    }

    public E getSelectedItem() {
        return selectedItems.get(0);
    }

    private void displayResults(final boolean first, final List<E> result) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                items.clear();
                items.addAll(result);

                if (first) {
                    content.remove(loading);
                    content.add(new JBScrollPane(table), BorderLayout.CENTER);
                    content.revalidate();
                    content.repaint();
                }

                model.fireTableDataChanged();
            }
        });
    }

    private void executeQuery(final boolean first, final String filter) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {

            @Override
            public void run() {
                ArrayList<E> items = new ArrayList<E>();
                if(provider.load(filter, items)) {
                    if(first) {
                        // truncated: if query changes execute fetch data again
                        header.addHeaderObserver(TableFilterDialog.this);
                        header.setInstantFiltering(false);
                        header.setFilterOnUpdates(false);
                    }
                    if(tooMany.getParent() == null) {
                        content.add(tooMany, BorderLayout.NORTH);
                        content.invalidate();
                        content.repaint();
                    }
                } else if(tooMany.getParent() != null) {
                    content.remove(tooMany);
                    content.invalidate();
                    content.repaint();
                }

                displayResults(first, items);
            }
        });
    }

    @Override
    public void tableFilterEditorCreated(TableFilterHeader tableFilterHeader, IFilterEditor iFilterEditor, TableColumn tableColumn) {
    }

    @Override
    public void tableFilterEditorExcluded(TableFilterHeader tableFilterHeader, IFilterEditor iFilterEditor, TableColumn tableColumn) {
    }

    @Override
    public void tableFilterUpdated(TableFilterHeader tableFilterHeader, IFilterEditor iFilterEditor, TableColumn tableColumn) {
        String filter = iFilterEditor.getContent().toString();
        if(!filter.endsWith("*")) {
            // enforce prefix semantics to remain compatible with instant search that is used when all items fit into the first (filter-less) request
            iFilterEditor.setContent(filter + "*");
        } else {
            executeQuery(false, filter);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(!e.getValueIsAdjusting()) {
            okButton.setEnabled(e.getFirstIndex() >= 0);
        }
    }

    private static class DefaultRenderer<E> implements ItemRenderer<E> {

        @Override
        public String toString(E item) {
            return item.toString();
        }
    }

    private class ListTableModel extends AbstractTableModel {

        public String getColumnName(int col) {
            return column;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        public Object getValueAt(int row, int col) {
            return renderer.toString(items.get(row));
        }
    }
}
