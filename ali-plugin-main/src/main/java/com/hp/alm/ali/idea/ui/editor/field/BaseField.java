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

package com.hp.alm.ali.idea.ui.editor.field;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseField implements EditableField {

    final private List<EditableFieldListener> listeners = new LinkedList<EditableFieldListener>();
    private String label;
    private boolean required;
    private String originalValue;

    public BaseField(String label, boolean required, String originalValue) {
        this.label = label;
        this.required = required;
        this.originalValue = originalValue;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void addUpdateListener(EditableFieldListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public void fireUpdated() {
        synchronized (listeners) {
            for(EditableFieldListener listener: listeners) {
                listener.updated(this);
            }
        }
    }

    @Override
    public boolean isDisableDefaultAction() {
        return false;
    }

    @Override
    public void setOriginalValue(String value) {
        originalValue = value;
        fireUpdated();
    }

    public String getOriginalValue() {
        return originalValue;
    }

    @Override
    public boolean hasChanged() {
        return !getValue().equals(getOriginalValue());
    }
}
