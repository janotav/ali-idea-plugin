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

import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MyRowSorter extends TableRowSorter<EntityTableModel> {

    public static final Comparator<Object> IDENTITY = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return 0;
        }
    };
    private boolean ignore;

    public MyRowSorter() {
        setMaxSortKeys(1);
    }

    public void setSortKeys(List<? extends SortKey> sortKeys) {
        if(ignore) {
            return;
        }
        super.setSortKeys(sortKeys);
    }

    public void toggleSortOrder(int col, boolean replace) {
        if(replace) {
            super.toggleSortOrder(col);
        } else {
            List<SortKey> sortKeys = new ArrayList<SortKey>(getSortKeys());
            for(int i = 0; i < sortKeys.size(); i++) {
                SortKey key = sortKeys.get(i);
                if(key.getColumn() == col) {
                    sortKeys.set(i, new SortKey(col, key.getSortOrder() == SortOrder.ASCENDING? SortOrder.DESCENDING: SortOrder.ASCENDING));
                    doSort(sortKeys);
                    return;
                }
            }
            sortKeys.add(new SortKey(col, SortOrder.ASCENDING));
            doSort(sortKeys);
        }

    }

    public void toggleSortOrder(int i) {
        // turn off default sort on click, we provide our own handler
    }

    private void doSort(List<SortKey> sortKeys) {
        setSortKeys(sortKeys);
        fireSortOrderChanged();
    }

    public Comparator<?> getComparator(int column) {
        // turn off all sorting (by using identity comparator) and use exclusive order returned from server
        return IDENTITY;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }
}
