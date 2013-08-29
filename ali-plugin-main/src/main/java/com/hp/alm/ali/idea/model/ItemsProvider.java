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

import java.util.List;

public interface ItemsProvider<E> {

    /**
     * Load items matching given filter.
     *
     * @param filter filter (can be null)
     * @param items list where items are placed
     * @return true if result was truncated (some items matching filter were not returned)
     */
    boolean load(String filter, List<E> items);

    public static class Eager<E> implements ItemsProvider<E> {

        private List<E> items;

        public Eager(List<E> items) {
            this.items = items;
        }

        @Override
        public boolean load(String filter, List<E> items) {
            // NOTE: no filtering is done all items are considered as matching filter
            items.addAll(this.items);
            return false;
        }
    }

    public static abstract class Loader<E> implements ItemsProvider<E> {

        public abstract List<E> load();

        @Override
        public boolean load(String filter, List<E> items) {
            // NOTE: no filtering is done all items are considered as matching the filter
            items.addAll(load());
            return false;
        }
    }
}
