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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.model.KeyValue;

public class ComboItem implements KeyValue<Object> {
    private Object key;
    private String displayValue;

    public ComboItem(String key) {
        this.key = key;
        this.displayValue = key;
    }

    public ComboItem(Object key, String displayValue) {
        this.key = key;
        this.displayValue = displayValue;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return getDisplayValue();
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String toString() {
        return getDisplayValue();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComboItem comboItem = (ComboItem) o;

        if (!key.equals(comboItem.key)) return false;

        return true;
    }

    public int hashCode() {
        return key.hashCode();
    }
}
