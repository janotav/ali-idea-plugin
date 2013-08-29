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

import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.combo.LazyComboBoxModel;
import com.intellij.openapi.project.Project;

import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class UserField extends BaseField {

    private JComboBox comboBox;
    private LazyComboBoxModel model;

    public UserField(String label, final String origValue, Project project, boolean required) {
        super(label, required, origValue);

        model = project.getComponent(RestService.class).getModelCustomization().getUserModel();
        comboBox = new JComboBox(model);
        comboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    fireUpdated();
                }
            }
        });
        if(!model.isReady()) {
            model.addListDataListener(new ListDataListener() {
                @Override
                public void intervalAdded(ListDataEvent e) {
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                    if(model.isReady()) {
                        // model is ready, try to select implicitly selected item
                        if(origValue != null) {
                            setValue(origValue);
                        }
                        model.removeListDataListener(this);
                    }
                }
            });
        } else if(origValue != null) {
            setValue(origValue);
        }
    }

    @Override
    public Component getComponent() {
        return comboBox;
    }

    @Override
    public String getValue() {
        return (String)((ComboItem)model.getSelectedItem()).getKey();
    }

    @Override
    public void setValue(String value) {
        model.setSelectedItem(new ComboItem(value, value));
    }
}
