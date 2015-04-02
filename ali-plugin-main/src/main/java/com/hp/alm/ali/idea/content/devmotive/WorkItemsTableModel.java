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

package com.hp.alm.ali.idea.content.devmotive;

import com.intellij.openapi.application.ApplicationManager;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkItemsTableModel extends AbstractTableModel {

    private List<WorkItem> workItems;

    private Map<WorkItem.Type, String> labelMap;

    public WorkItemsTableModel() {
        workItems = new ArrayList<WorkItem>();

        labelMap = new HashMap<WorkItem.Type, String>();
        labelMap.put(WorkItem.Type.DEFECT, "Defect");
        labelMap.put(WorkItem.Type.USER_STORY, "User Story");
        labelMap.put(WorkItem.Type.NONE, "");
    }

    @Override
    public int getRowCount() {
        return workItems.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Type";

            case 1:
                return "ID";

            default:
                return "Name";
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 2:
                return WorkItem.class;
            default:
                return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public Object getValueAt(final int rowIndex, int columnIndex) {
        final WorkItem workItem = getWorkItem(rowIndex);
        switch (columnIndex) {
            case 0:
                return labelMap.get(workItem.getType());

            case 1:
                return workItem.getId();
        }

        return workItem;
    }

    public List<WorkItem> getWorkItems() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        return workItems;
    }

    public WorkItem getWorkItem(int rowIndex) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        return workItems.get(rowIndex);
    }

    public int addWorkItem(WorkItem workItem) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        int idx = workItems.indexOf(workItem);
        if (idx < 0) {
            int count = getRowCount();
            workItems.add(workItem);
            fireTableRowsInserted(count, count);
            return count;
        } else {
            return idx;
        }
    }

    public void removeWorkItem(WorkItem workItem) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        int p = workItems.indexOf(workItem);
        if (p >= 0) {
            workItems.remove(p);
            fireTableRowsDeleted(p, p);
        }
    }
}
