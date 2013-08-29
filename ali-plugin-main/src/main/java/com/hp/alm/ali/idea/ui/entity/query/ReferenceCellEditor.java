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


import com.hp.alm.ali.idea.entity.EntityFilterTableModel;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import java.awt.Component;

public class ReferenceCellEditor extends AbstractCellEditor implements TableCellEditor {
    private FilterFactory factory;
    private EntityFilterTableModel tableModel;
    private FilterChooser popup;
    private JLabel component;

    public ReferenceCellEditor(FilterFactory factory, EntityFilterTableModel tableModel) {
        this.factory = factory;
        this.tableModel = tableModel;

        component = new JLabel();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
        final int modelRow = table.convertRowIndexToModel(row);
        popup = factory.createChooser(tableModel.getUserFilter(modelRow));
        popup.show();
        String selectedValue = popup.getSelectedValue();
        tableModel.setUserFilter(modelRow, selectedValue);
        component.setText(tableModel.getUserFilter(modelRow));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tableModel.fireTableDataChanged(); // ask for translation (note: row/cell update does not work)
            }
        });
        return component;
    }

    public String getCellEditorValue() {
        return popup.getSelectedValue();
    }
}
