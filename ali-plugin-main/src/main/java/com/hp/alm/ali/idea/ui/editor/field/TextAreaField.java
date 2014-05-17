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
import javax.swing.JTextPane;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class TextAreaField extends BaseField {

    private JTextPane textPane;

    public TextAreaField(String label, String origValue, boolean required, boolean editable) {
        super(label, required, origValue);

        textPane = new JTextPane();
        textPane.setText(origValue);
        textPane.setEditable(editable);

        // there's no border by default? is it a bug?
        textPane.setBorder(new JTextField().getBorder());
        // if there's border it doesn't react to focus? bug again?
        textPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
            private void repaint() {
                textPane.invalidate();
                textPane.repaint();
            }
        });

        HTMLAreaField.installNavigationShortCuts(textPane);

        if(editable) {
            textPane.getDocument().addDocumentListener(new MyDocumentListener(this));
            UndoAction.installUndoRedoSupport(textPane);
        }
    }

    @Override
    public Component getComponent() {
        return textPane;
    }

    @Override
    public String getValue() {
        return textPane.getText();
    }

    @Override
    public void setValue(String value) {
        textPane.setText(value);
    }
}
