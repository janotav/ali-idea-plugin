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

package com.hp.alm.ali.idea.cfg;

import javax.swing.JPasswordField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

class MergedPasswordField extends JPasswordField implements FocusListener, MergingField {
    private String defaultValue;

    public MergedPasswordField(int width, String defaultValue) {
        super(width);
        this.defaultValue = defaultValue;

        addFocusListener(this);
    }

    public void focusGained(FocusEvent focusEvent) {
        MergedTextField.onFocusGained(this, defaultValue);
    }

    public void focusLost(FocusEvent focusEvent) {
        MergedTextField.onFocusLost(this, defaultValue);
    }

    public String getValue() {
        return MergedTextField.getValue(this, defaultValue);
    }

    public void setValue(String value) {
        MergedTextField.setValue(this, value, defaultValue);
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
        MergedTextField.setDefaultValue(this, value);
    }
}

