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

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;

public class SpinnerField extends BaseField {

    private JSpinner spinner;

    public SpinnerField(String label, String origValue, boolean required) {
        super(label, required, origValue);

        spinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(origValue).intValue(), 0, Integer.MAX_VALUE, 1));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fireUpdated();
            }
        });
    }

    @Override
    public Component getComponent() {
        return spinner;
    }

    @Override
    public String getValue() {
        return spinner.getValue().toString();
    }

    @Override
    public void setValue(String value) {
        spinner.setValue(Integer.valueOf(value));
    }
}
