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

package com.hp.alm.ali.idea.entity;

import java.util.LinkedList;
import java.util.List;

public class FilterModelImpl<E extends EntityFilter> implements EntityFilterModel<E> {

    final private List<FilterListener<E>> filterListeners = new LinkedList<FilterListener<E>>();
    final private E filter;

    public FilterModelImpl(E filter) {
        this.filter = filter;
    }

    @Override
    public void addFilterListener(FilterListener listener) {
        synchronized (filterListeners) {
            filterListeners.add(listener);
        }
    }

    @Override
    public E getFilter() {
        return filter;
    }

    @Override
    public void fireFilterUpdated(boolean dataChanged) {
        synchronized (filterListeners) {
            for(FilterListener<E> listener: filterListeners) {
                listener.filterChanged(filter);
            }
        }
    }
}
