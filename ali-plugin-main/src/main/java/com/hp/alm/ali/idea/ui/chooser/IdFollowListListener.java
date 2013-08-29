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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.table.EntityTableModel;

import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

class IdFollowListListener implements ListSelectionListener {

    private JTextField valueField;
    private EntityTableModel model;

    public IdFollowListListener(JTextField valueField, EntityTableModel model) {
        this.valueField = valueField;
        this.model = model;
    }

    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if(!listSelectionEvent.getValueIsAdjusting()) {
            int row = ((ListSelectionModel)listSelectionEvent.getSource()).getMinSelectionIndex();
            if(row >= 0) {
                Entity entity = model.getEntity(row);
                valueField.setText(String.valueOf(entity.getId()));
            } else {
                valueField.setText("");
            }
        }
    }
}
