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
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class CommitsTableModel extends AbstractTableModel {

    private List<Commit> commits = new ArrayList<Commit>();

    @Override
    public int getRowCount() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        return commits.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Date.class;
            default:
                return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Date";
            case 1:
                return "Author";
            default:
                return "Message";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Commit commit = getCommit(rowIndex);
        switch (columnIndex) {
            case 0:
                return commit.getDate();

            case 1:
                return commit.getAuthorName();

            default:
                return commit.getMessage();
        }
    }

    public Commit getCommit(int row) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        return commits.get(row);
    }

    public void add(Collection<Commit> commits) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        for (Commit commit: commits) {
            if (!this.commits.contains(commit)) {
                int count = this.commits.size();
                this.commits.add(commit);
                fireTableRowsInserted(count, count);
            }
        }
    }

    public void remove(Commit commit) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        int p = commits.indexOf(commit);
        if (p >= 0) {
            commits.remove(p);
            fireTableRowsDeleted(p, p);
        }
    }

    public void remove(Collection<Commit> commits) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        for (Commit commit: commits) {
            remove(commit);
        }
    }
}
