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

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class QuickSearchPanel extends JPanel {

    private JTextField findField;
    private JLabel findLink;
    private JLabel searchingLabel;
    private LinkLabel clearLink;
    private String currentFilter;

    private Target target;

    public QuickSearchPanel(String value, Target target, final boolean instant) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        this.target = target;

        findField = new JTextField(value, 10);
        add(findField);
        clearLink = new LinkLabel("", IconLoader.getIcon("/actions/close.png"));
        clearLink.setHoveringIcon(IconLoader.getIcon("/actions/closeHovered.png"));
        clearLink.setListener(new LinkListener() {
            @Override
            public void linkSelected(LinkLabel linkLabel, Object o) {
                findField.setText("");
                if(!instant) {
                    doFilter();
                }
            }
        }, null);
        clearLink.setToolTipText("Clear filter");
        clearLink.setVisible(!getValue().isEmpty());
        add(clearLink);
        if(instant) {
            findLink = new JLabel(IconLoader.getIcon("/actions/find.png"));
            findLink.setToolTipText("Enter filter to the field on the left to narrow the results");
            add(findLink);
            findField.getDocument().addDocumentListener(new DocumentListener() {
                private void updated() {
                    doFilter();
                }
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updated();
                }
            });
            installEscapeAction(findField, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    findField.setText("");
                }
            });
        } else {
            findField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    doFilter();
                }
            });
            findLink = new LinkLabel("", IconLoader.getIcon("/actions/find.png"));
            findLink.setToolTipText("Search");
            ((LinkLabel)findLink).setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel linkLabel, Object o) {
                    String value = findField.getText();
                    clearLink.setVisible(!value.isEmpty());
                    findLink.setVisible(value.isEmpty());
                    QuickSearchPanel.this.target.executeFilter(findField.getText());
                }
            }, null);
            add(findLink);
            findField.getDocument().addDocumentListener(new DocumentListener() {
                private void updated() {
                    String value = findField.getText();
                    clearLink.setVisible(currentFilter.equals(value) && !currentFilter.isEmpty());
                    findLink.setVisible(!clearLink.isVisible());
                }
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updated();
                }
            });
            installEscapeAction(findField, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    findField.setText(currentFilter);
                }
            });
        }
        findLink.setVisible(!clearLink.isVisible());
        currentFilter = findField.getText();

        searchingLabel = new JLabel("Loading...");
        searchingLabel.setVisible(false);
        add(searchingLabel);
    }

    private void installEscapeAction(JComponent comp, AbstractAction escapeAction) {
        comp.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        comp.getActionMap().put("escape", escapeAction);
    }

    private void doFilter() {
        currentFilter = findField.getText();
        clearLink.setVisible(!currentFilter.isEmpty());
        findLink.setVisible(currentFilter.isEmpty());
        QuickSearchPanel.this.target.executeFilter(currentFilter);
    }

    public boolean hasValue() {
        return !findField.getText().isEmpty();
    }

    public String getValue() {
        return findField.getText();
    }

    public void clear() {
        findField.setText("");
        findField.postActionEvent();
    }

    public void setRunning(boolean running) {
        findField.setEnabled(!running);
        findLink.setEnabled(!running);
        clearLink.setEnabled(!running);
        searchingLabel.setVisible(running);
    }

    public interface Target {

        void executeFilter(String value);

    }
}
