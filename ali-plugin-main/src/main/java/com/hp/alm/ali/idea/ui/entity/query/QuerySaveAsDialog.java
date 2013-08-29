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

package com.hp.alm.ali.idea.ui.entity.query;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class QuerySaveAsDialog extends JDialog implements ActionListener {
    private JTextField target;
    final JButton ok;
    private boolean success = false;

    public QuerySaveAsDialog(final List<String> existing) {
        super(new JFrame(), "Save Query As", true);

        JPanel buttons = new JPanel(new FlowLayout());
        ok = new JButton("OK");
        ok.setEnabled(false);
        ok.addActionListener(this);
        buttons.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
                dispose();
            }
        });
        buttons.add(cancel);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new GridLayout(0, 1));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        content.add(new JLabel("Query Name: "));
        target = new JTextField(20);
        target.addActionListener(this);
        final JComboBox list = new JComboBox(existing.toArray());
        list.setSelectedIndex(-1);
        target.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent documentEvent) {
                update();
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                update();
            }

            public void changedUpdate(DocumentEvent documentEvent) {
                update();
            }

            private void update() {
                int i = existing.indexOf(target.getText());
                if (list.getSelectedIndex() != i) {
                    list.setSelectedIndex(i);
                }
                ok.setEnabled(!target.getText().isEmpty());
            }
        });
        content.add(target);
        if(!existing.isEmpty()) {
            content.add(new JLabel("Update Existing: "));
            list.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                        String text = (String) itemEvent.getItem();
                        if (!target.getText().equals(text)) {
                            target.setText(text);
                        }
                    }
                }
            });
            content.add(list);
        }
        getContentPane().add(content, BorderLayout.WEST);
        getContentPane().add(new JPanel(), BorderLayout.CENTER);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public String getTargetName() {
        setVisible(true);
        return success? target.getText() : null;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if(ok.isEnabled()) {
            setVisible(false);
            dispose();
            success = true;
        }
    }
}
