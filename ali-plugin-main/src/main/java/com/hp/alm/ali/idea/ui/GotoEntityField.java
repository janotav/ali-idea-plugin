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

import com.hp.alm.ali.idea.content.EntityContentPanel;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class GotoEntityField extends JPanel {

    private static Icon gotoIcon = IconLoader.getIcon("/goto_16.png");

    public static final String GOTO = "Go To...";

    private JTextField gotoField;
    private final String entityType;
    private final EntityContentPanel.EntityAction action;

    public GotoEntityField(final String entityType, final EntityContentPanel.EntityAction action) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.entityType = entityType;
        this.action = action;

        gotoField = new JTextField(6);
        add(gotoField);
        final LinkLabel gotoLink = new LinkLabel("", gotoIcon);
        add(gotoLink);
        gotoLink.setEnabled(false);
        gotoField.setText(GOTO);
        gotoField.setForeground(Color.GRAY);
        gotoField.getDocument().addDocumentListener(new DocumentListener() {
            private void enableDisable() {
                Container parent = getParent();
                try {
                    Integer.valueOf(gotoField.getText());
                    if (!gotoLink.isEnabled()) {
                        gotoLink.setEnabled(true);
                        if (parent instanceof JComponent) {
                            ((JComponent) parent).revalidate();
                        }
                        parent.repaint();
                    }
                } catch (Exception e) {
                    if (gotoLink.isEnabled()) {
                        gotoLink.setEnabled(false);
                        if (parent instanceof JComponent) {
                            ((JComponent) parent).revalidate();
                        }
                        parent.repaint();
                    }
                }
            }

            public void insertUpdate(DocumentEvent documentEvent) {
                enableDisable();
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                enableDisable();
            }

            public void changedUpdate(DocumentEvent documentEvent) {
                enableDisable();
            }
        });
        gotoField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent focusEvent) {
                if (GOTO.equals(gotoField.getText())) {
                    gotoField.setText("");
                    gotoField.setForeground(Color.BLACK);
                }
            }

            public void focusLost(FocusEvent focusEvent) {
                if (gotoField.getText().isEmpty()) {
                    gotoField.setText(GOTO);
                    gotoField.setForeground(Color.GRAY);
                }
            }
        });
        gotoField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                go();
            }
        });
        gotoLink.setListener(new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object o) {
                go();
            }
        }, null);
    }

    private void go() {
        int id;
        try {
            id = Integer.valueOf(gotoField.getText());
        } catch(NumberFormatException e) {
            return;
        }
        Entity entity = new Entity(entityType, id);
        action.perform(entity);
    }
}
