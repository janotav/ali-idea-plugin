/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.intellij.openapi.project.Project;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class EntityChooser extends MyDialog implements FilterChooser {

    protected String selectedValue;
    protected JTextField valueField;
    protected Project project;
    protected String entityType;

    public EntityChooser(Project project, String entityType, boolean showCondition, boolean acceptEmpty) {
        super(project, new JFrame(), "Select {0}", true, false, Arrays.asList(Button.OK, Button.Close));

        this.project = project;
        this.entityType = entityType;

        initialize(showCondition, acceptEmpty);
    }

    public String getSelectedValue() {
        return selectedValue == null? "": selectedValue;
    }

    public void setValue(String s) {
        selectedValue = s;
        valueField.setText(s);
    }

    protected void buttonPerformed(Button button) {
        super.buttonPerformed(button);

        switch (button) {
            case OK:
                selectedValue = valueField.getText();
                close(true);
                break;

            case Clear:
                selectedValue = "";
                close(true);
                break;
        }
    }

    private void initialize(boolean showCondition, final boolean acceptEmpty) {
        setEditorTitle(project, "Select: {0}", entityType);

        JPanel conditionPanel = new JPanel(new BorderLayout());
        conditionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(10, 10, 10, 10)));
        JLabel label = new JLabel("Condition:");
        label.setBorder(new EmptyBorder(0, 0, 0, 10));
        conditionPanel.add(label, BorderLayout.WEST);
        valueField = new JTextField();
        conditionPanel.add(valueField, BorderLayout.CENTER);

        final JButton ok = getButton(Button.OK);
        ok.setEnabled(!valueField.getText().isEmpty());
        valueField.getDocument().addDocumentListener(new DocumentListener() {
            private void toggleOk() {
                ok.setEnabled(!valueField.getText().isEmpty());
            }
            public void insertUpdate(DocumentEvent documentEvent) {
                toggleOk();
            }
            public void removeUpdate(DocumentEvent documentEvent) {
                toggleOk();
            }
            public void changedUpdate(DocumentEvent documentEvent) {
                toggleOk();
            }
        });
        if(acceptEmpty) {
            addButton(Button.Clear);
        }

        if(showCondition) {
            getContentPane().add(conditionPanel, BorderLayout.NORTH);
            valueField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if(ok.isEnabled()) {
                        buttonPerformed(Button.OK);
                    }
                }
            });
        }

        setPreferredSize(new Dimension(600, 400));
        setSize(new Dimension(600, 400));
    }
}
