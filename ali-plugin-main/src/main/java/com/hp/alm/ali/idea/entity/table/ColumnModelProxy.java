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

package com.hp.alm.ali.idea.entity.table;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

public class ColumnModelProxy implements TableColumnModelListener {

    private EntityTableModel model;

    public ColumnModelProxy(EntityTableModel model) {
        this.model = model;
    }

    public void columnAdded(TableColumnModelEvent tableColumnModelEvent) {
    }

    public void columnRemoved(TableColumnModelEvent tableColumnModelEvent) {
    }

    public void columnMoved(TableColumnModelEvent tableColumnModelEvent) {
        model.updateColumnsFromView();
    }

    public void columnMarginChanged(ChangeEvent changeEvent) {
        model.updateColumnsFromView();
    }

    public void columnSelectionChanged(ListSelectionEvent listSelectionEvent) {
    }
}
