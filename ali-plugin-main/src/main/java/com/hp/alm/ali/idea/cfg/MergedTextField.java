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

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

class MergedTextField extends JTextField implements FocusListener, MergingField {
    private String defaultValue;

    public MergedTextField(int width, String defaultValue) {
        super(width);
        this.defaultValue = defaultValue;

        addFocusListener(this);
    }

    public void focusGained(FocusEvent focusEvent) {
        onFocusGained(this, defaultValue);
    }

    public void focusLost(FocusEvent focusEvent) {
        onFocusLost(this, defaultValue);
    }

    public String getValue() {
        return getValue(this, defaultValue);
    }

    public void setValue(String value) {
        setValue(this, value, defaultValue);
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
        setDefaultValue(this, value);
    }

    static void onFocusGained(JTextComponent comp, String defaultValue) {
        if(comp.getForeground().equals(Color.GRAY)) {
            comp.setForeground(Color.BLACK);
            comp.setText(defaultValue);
        }
    }

    static void onFocusLost(JTextComponent comp, String defaultValue) {
        if(comp.getText().isEmpty() || comp.getText().equals(defaultValue)) {
            comp.setForeground(Color.GRAY);
            comp.setText(defaultValue);
        }
    }

    static String getValue(JTextComponent comp, String defaultValue) {
        if(comp.getText().equals(defaultValue)) {
            return "";
        } else {
            return comp.getText();
        }
    }

    static void setValue(JTextComponent comp, String value, String defaultValue) {
        if("".equals(value) || value.equals(defaultValue)) {
            if(!comp.hasFocus()) {
                comp.setForeground(Color.GRAY);
            }
            comp.setText(defaultValue);
        } else {
            comp.setForeground(Color.BLACK);
            comp.setText(value);
        }
    }

    static void setDefaultValue(JTextComponent comp, String value) {
        if(comp.getForeground().equals(Color.GRAY)) {
            comp.setText(value);
        }
    }
}
