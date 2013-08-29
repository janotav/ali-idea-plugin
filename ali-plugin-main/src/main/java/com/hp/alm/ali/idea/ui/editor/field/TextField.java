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

import com.hp.alm.ali.idea.action.UndoAction;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Component;

public class TextField extends BaseField {

    private JTextComponent textField;

    public TextField(String label, String value, boolean required, boolean editable) {
        this(new JTextField(value, 12), label, required, editable);
    }

    public TextField(JTextComponent textField, String label, boolean required, boolean editable) {
        super(label, required, textField.getText());

        this.textField = textField;

        // HTMLAreaField overrides editable flag when unsupported element is encountered
        editable &= textField.isEditable();

        textField.setEnabled(editable);
        textField.getDocument().addDocumentListener(new MyDocumentListener(this));

        if(editable) {
            UndoAction.installUndoRedoSupport(textField);
        }
    }

    public Component getComponent() {
        return textField;
    }

    public String getValue() {
        return textField.getText();
    }

    @Override
    public void setValue(String value) {
        textField.setText(value);
    }
}
