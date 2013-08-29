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

package com.hp.alm.ali.idea.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AuditModel extends AbstractTableModel {

    private List<String[]> properties;

    public AuditModel(List<String[]> properties) {
        this.properties = new ArrayList<String[]>(properties);
    }

    public int getRowCount() {
        return properties.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public Class getColumnClass(int col) {
        return String.class;
    }

    public String getValueAt(int row, int col) {
        return properties.get(row)[col];
    }

    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Field";

            case 1:
                return "Old Value";

            case 2:
                return "New Value";
        }
        return "";
    }
}
