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

import com.hp.alm.ali.idea.entity.table.EntityTableModel;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

public class LookupCellEditor extends DefaultCellEditor {

    private List<String> values;

    public LookupCellEditor(List<String> list, boolean quote) {
        super(new JComboBox());
        if(quote) {
            values = EntityTableModel.quote(list);
        } else {
            values = list;
        }
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
        JComboBox combo = (JComboBox)getComponent();
        combo.setEditable(false);
        combo.removeAllItems();
        combo.addItem("");
        for(String option: values) {
            combo.addItem(option);
        }
        combo.setSelectedItem(value);
        return super.getTableCellEditorComponent(table, value, isSelected, row, col);
    }
}

