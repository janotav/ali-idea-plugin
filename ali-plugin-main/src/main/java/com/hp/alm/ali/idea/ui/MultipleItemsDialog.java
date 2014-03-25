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
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import net.coderazzi.filters.gui.IFilterEditor;
import net.coderazzi.filters.gui.IFilterHeaderObserver;
import net.coderazzi.filters.gui.TableFilterHeader;
import org.apache.commons.lang.ArrayUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIDefaults;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MultipleItemsDialog<K, E extends KeyValue<K>> extends MyDialog implements IFilterHeaderObserver {

    private JLabel tooMany;
    private JLabel selected;
    private JToggleButton toggleSelected;
    private JButton okButton;
    private boolean ok;
    private MultipleItemsDialogModel<K, E> model;
    private JBTable table;
    private TableFilterHeader header;
    private MySelectionModel mySelectionModel;
    private MyListSelectionListener myListSelectionListener;

    public MultipleItemsDialog(Project project, String title, final boolean multiple, final List<E> fieldList, final List<K> selectedFields) {
        this(project, "Select " + title, new MultipleItemsDialogModel<K, E>(title, multiple, new ItemsProvider.Eager<E>(fieldList), selectedFields, null));
    }

    public MultipleItemsDialog(Project project, String title, final MultipleItemsDialogModel<K, E> model) {
        super(project, new JFrame(), title, true);

        this.model = model;

        mySelectionModel = new MySelectionModel();
        myListSelectionListener = new MyListSelectionListener();

        tooMany = new JLabel("Too many results, narrow your search");
        tooMany.setBorder(BorderFactory.createEtchedBorder());
        tooMany.setVisible(false);
        selected = new JLabel("Showing currently selected items");
        selected.setVisible(false);
        toggleSelected = new JToggleButton();
        toggleSelected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setShowingSelected(toggleSelected.isSelected());
                if(!model.isShowingSelected() && !model.getSelectedFields().isEmpty()) {
                    updateSelectionFromModel();
                } else if(model.isShowingSelected()) {
                    header.getFilterEditor(1).setContent("");
                }
            }
        });
        updateSelected();

        table = new JBTable() {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                int column = convertColumnIndexToModel(columnIndex);
                mySelectionModel.setFirstColumnEvent(column == 0);
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setAutoCreateColumnsFromModel(false);
        table.setModel(model);
        final MyTableRowSorter sorter = new MyTableRowSorter(model);
        table.setRowSorter(sorter);
        table.setDefaultRenderer(Boolean.class, new MyRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setSelectionModel(mySelectionModel);

        sorter.setIgnoreAddRowSorterListener(true); // prevent auto-selection (functionality not accessible via proper API)
        header = new TableFilterHeader(table);
        sorter.setIgnoreAddRowSorterListener(false);

        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.ASCENDING)));
        JPanel panel = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEtchedBorder());
        panel.add(toolbar, BorderLayout.NORTH);
        toolbar.add(toggleSelected, BorderLayout.EAST);

        if(model.isMultiple()) {
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            table.getColumnModel().addColumn(createColumn(0, model, 45, 45));
            header.getFilterEditor(0).setEditable(false);
            header.getFilterEditor(0).setUserInteractionEnabled(false);

            final LinkListener selectUnselect = new LinkListener() {
                public void linkSelected(LinkLabel aSource, Object aLinkData) {
                    if(model.isShowingSelected()) {
                        if(!Boolean.TRUE.equals(aLinkData)) {
                            List<Integer> ixs = new ArrayList<Integer>();
                            for (int i = 0; i < sorter.getViewRowCount(); i++) {
                                ixs.add(sorter.convertRowIndexToModel(i));
                            }
                            // make sure indexes are not affected by removal by starting from the last
                            Collections.sort(ixs);
                            Collections.reverse(ixs);
                            for(int ix: ixs) {
                                model.setValueAt(aLinkData, ix, 0);
                            }
                        }
                    } else {
                        if(Boolean.TRUE.equals(aLinkData)) {
                            mySelectionModel.doAddSelectionInterval(0, table.getRowCount() - 1);
                        } else {
                            mySelectionModel.removeSelectionInterval(0, table.getRowCount() - 1);
                        }
                    }
                }
            };

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
            left.add(new LinkLabel("Select All", IconLoader.getIcon("/actions/selectall.png"), selectUnselect, true));
            left.add(new LinkLabel("Unselect All", IconLoader.getIcon("/actions/unselectall.png"), selectUnselect, false));
            toolbar.add(left, BorderLayout.WEST);
        } else {
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        table.getColumnModel().addColumn(createColumn(1, model, 450, null));
        table.getSelectionModel().addListSelectionListener(myListSelectionListener);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                selected.setVisible(model.isShowingSelected());
                tooMany.setVisible(model.hasMore() && !model.isShowingSelected());
                updateSelected();
            }
        });

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(selected, BorderLayout.NORTH);
        contentPanel.add(new JBScrollPane(table), BorderLayout.CENTER);
        contentPanel.add(tooMany, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ok = true;
                close(true);
            }
        });
        buttons.add(okButton);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                close(false);
            }
        });
        buttons.add(cancel);
        panel.add(buttons, BorderLayout.SOUTH);
        getContentPane().add(panel, BorderLayout.CENTER);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        requestPropertyFilterFocus(header);

        load(true, null);
    }

    private void updateSelectionFromModel() {
        // try to highlight selected item when switching back from selected view
        myListSelectionListener.setIgnoreUpdateEvent(true);
        try {
            for (int i = 0; i < table.getRowCount(); i++) {
                int modelRow = table.convertRowIndexToModel(i);
                E value = model.getFields().get(modelRow);
                if(model.getSelectedFields().contains(value.getKey())) {
                    mySelectionModel.doAddSelectionInterval(i, i);
                    if(!model.isMultiple()) {
                        break;
                    }
                }
            }
        } finally {
            myListSelectionListener.setIgnoreUpdateEvent(false);
        }
    }

    private void updateOk() {
        okButton.setEnabled(model.isMultiple() || !model.getSelectedFields().isEmpty());
    }

    private void updateSelected() {
        int size = model.getSelectedFields().size();
        if(size > 0) {
            toggleSelected.setVisible(true);
            if(model.isMultiple()) {
                toggleSelected.setText("Show Selected (" + size + ")");
            } else {
                toggleSelected.setText("Show Selected");
            }
        } else {
            toggleSelected.setSelected(false);
            toggleSelected.setVisible(false);
            model.setShowingSelected(false);
        }
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
        if(!filter.isEmpty() && !filter.endsWith("*")) {
            // enforce prefix semantics to remain compatible with instant search that is used when all items fit into the first (filter-less) request
            iFilterEditor.setContent(filter + "*");
        } else {
            load(false, filter);
        }
    }

    private void load(final boolean first, final String filter) {
        final StatusText emptyText = table.getEmptyText();
        final String originalValue = emptyText.getText();
        emptyText.setText("Loading...");
        okButton.setEnabled(false);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                model.load(filter);
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        if (model.hasMore() && first) {
                            // truncated: if query changes execute fetch data again
                            header.addHeaderObserver(MultipleItemsDialog.this);
                            header.setInstantFiltering(false);
                            header.setFilterOnUpdates(false);
                        }
                        emptyText.setText(originalValue);
                        updateSelectionFromModel();
                        updateOk();
                    }
                });
            }
        });
    }


    private void requestPropertyFilterFocus(TableFilterHeader header) {
        IFilterEditor editor = header.getFilterEditor(model.getColumnCount() - 1);
        if(editor instanceof JComponent) {
            if(((JComponent) editor).getComponentCount() == 2) {
                ((JComponent) editor).getComponent(1).requestFocus();
            }
        }
    }

    private TableColumn createColumn(int index, TableModel model, int preferredWidth, Integer minWidth) {
        TableColumn column = new TableColumn(index);
        column.setPreferredWidth(preferredWidth);
        if(minWidth != null) {
            column.setMinWidth(minWidth);
        }
        column.setHeaderValue(model.getColumnName(index));
        return column;
    }

    public boolean isOk() {
        return ok;
    }

    private class MyRenderer extends JCheckBox implements TableCellRenderer {

        private Color selectedColor;

        public MyRenderer() {
            setHorizontalAlignment(JCheckBox.CENTER);
            UIDefaults defaults = javax.swing.UIManager.getDefaults();
            selectedColor = defaults.getColor("List.selectionBackground");
        }

        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int colIndex) {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(table.isRowSelected(rowIndex)? selectedColor: Color.WHITE);
            return this;
        }
    }

    private static class MyTableRowSorter extends TableRowSorter<TableModel> {

        private boolean ignoreAddRowSorterListener;

        public MyTableRowSorter(TableModel model) {
            super(model);
        }

        @Override
        public void addRowSorterListener(RowSorterListener l) {
            if(!ignoreAddRowSorterListener) {
                super.addRowSorterListener(l);
            }
        }

        public void setIgnoreAddRowSorterListener(boolean ignore) {
            this.ignoreAddRowSorterListener = ignore;
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        private boolean ignoreUpdateEvent;

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting() && !model.isShowingSelected()) {
                update();
            }
        }

        public void setIgnoreUpdateEvent(boolean ignoreUpdateEvent) {
            this.ignoreUpdateEvent = ignoreUpdateEvent;
            update();
        }

        private void update() {
            if(!ignoreUpdateEvent) {
                HashSet<Integer> selectedRows = new HashSet<Integer>(Arrays.asList(ArrayUtils.toObject(table.getSelectedRows())));
                for (int i = 0; i < table.getRowCount(); i++) {
                    int modelRow = table.convertRowIndexToModel(i);
                    model.setValueAt(selectedRows.contains(i), modelRow, 0);
                }
                updateOk();
            }
        }
    }

    private class MySelectionModel extends DefaultListSelectionModel {

        private boolean firstColumnEvent;
        private boolean passThrough;

        public void setFirstColumnEvent(boolean firstColumnEvent) {
            this.firstColumnEvent = firstColumnEvent;
            this.passThrough = false;
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if(!passThrough && firstColumnEvent) {
                toggleRowSelection(index1);
            } else {
                super.setSelectionInterval(index0, index1);
            }
        }

        private void toggleRowSelection(int rowIndex) {
            // avoid special handling on recursive calls
            passThrough = true;

            // when clicking on the checkbox column only consider the row where user clicked (index1)
            K key = model.getFields().get(table.convertRowIndexToModel(rowIndex)).getKey();
            if(model.getSelectedFields().contains(key)) {
                super.removeSelectionInterval(rowIndex, rowIndex);
            } else {
                super.addSelectionInterval(rowIndex, rowIndex);
            }
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            if(!passThrough && firstColumnEvent) {
                toggleRowSelection(index1);
            } else {
                super.addSelectionInterval(index0, index1);
            }
        }

        public void doAddSelectionInterval(int index0, int index1) {
            super.addSelectionInterval(index0, index1);
        }
    }
}
