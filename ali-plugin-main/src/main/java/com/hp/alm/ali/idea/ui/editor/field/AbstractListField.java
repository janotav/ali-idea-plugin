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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public abstract class AbstractListField extends BaseField {

    private JComboBox combo;

    public AbstractListField(String label, boolean required, boolean editable) {
        super(label, required, null);

        combo = new JComboBox();
        combo.setEnabled(editable);
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    fireUpdated();
                }
            }
        });
    }

    protected void initializeValues(List<String> options, String origValue) {
        setOriginalValue(origValue);
        for(String option: options) {
            combo.addItem(option);
        }
        if("".equals(origValue) && isRequired()) {
            // never offer no value for required fields
            combo.setSelectedIndex(-1);
        } else {
            // otherwise offer original value (even if not listed)
            if(!options.contains(origValue)) {
                combo.addItem(origValue);
            }
            combo.setSelectedItem(origValue);
        }
    }

    protected String getSelectedItem() {
        return (String)combo.getSelectedItem();
    }

    public Component getComponent() {
        return combo;
    }

    public void setValue(String value) {
        combo.setSelectedItem(value);
    }
}
