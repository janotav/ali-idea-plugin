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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.tree.HierarchicalModelFactory;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.entity.tree.HierarchicalEntityModel;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class PopupDialog extends MyDialog implements FilterChooser {

    public static enum Selection {
        SINGLE,
        APPENDING,
        APPENDING_ID,
        FOLLOW,
        FOLLOW_ID
    }

    private String selectedValue;
    private TreePanel treePanel;
    private EntityTable entityTable;
    private JTextField valueField;
    private HierarchicalEntityModel treeModel;
    private Project project;
    private String entityType;

    public PopupDialog(Project project, String entityType) {
        this(project, entityType, false, true, Selection.APPENDING, true);
    }

    public PopupDialog(Project project, String entityType, boolean hideRoot, boolean showCondition, Selection selectionMode, final boolean acceptEmpty) {
        this(project, entityType, "Select {0}");

        initialize(showCondition, acceptEmpty);
        if(Metadata.getChildEntity(entityType) != null || Metadata.getParentEntity(entityType) != null) {
            initializeHierarchical(selectionMode, hideRoot, null);
        } else {
            initializeFlat(selectionMode);
        }
    }

    public PopupDialog(Project project, String entityType, boolean hideRoot, boolean showCondition, Selection selectionMode, final boolean acceptEmpty, HierarchicalEntityModel treeModel) {
        this(project, entityType, "Select {0}");

        initialize(showCondition, acceptEmpty);
        initializeHierarchical(selectionMode, hideRoot, treeModel);
    }

    private PopupDialog(Project project, String entityType, String template) {
        super(project, new JFrame(), template, true, false, Arrays.asList(Button.OK, Button.Close));

        this.project = project;
        this.entityType = entityType;
    }


    public String getSelectedValue() {
        return selectedValue == null? "": selectedValue;
    }

    public void setValue(String s) {
        selectedValue = s;
        valueField.setText(s);
    }

    public HierarchicalEntityModel getModel() {
        return treeModel;
    }

    public boolean isFlat() {
        return treeModel == null;
    }

    private void initializeFlat(Selection selectionMode) {
        entityTable = new EntityTable(project, entityType, project.getComponent(AliProjectConfiguration.class).getLookupFilter(entityType));
        if(selectionMode == Selection.APPENDING || selectionMode == Selection.APPENDING_ID) {
            entityTable.getTable().getSelectionModel().addListSelectionListener(new AppendingListListener(valueField, entityTable.getModel()));
        } else if(selectionMode == Selection.FOLLOW || selectionMode == Selection.FOLLOW_ID || selectionMode == Selection.SINGLE) {
            entityTable.getTable().getSelectionModel().addListSelectionListener(new IdFollowListListener(valueField, entityTable.getModel()));
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(entityTable, BorderLayout.CENTER);
        panel.add(entityTable.getStatusComponent(), BorderLayout.SOUTH);
        getContentPane().add(panel, BorderLayout.CENTER);
    }

    private void initializeHierarchical(Selection selectionMode, boolean hideRoot, HierarchicalEntityModel treeModel) {
        if(treeModel == null) {
            this.treeModel = project.getComponent(HierarchicalModelFactory.class).createModel(entityType);
        } else {
            this.treeModel = treeModel;
        }
        treePanel = new TreePanel(project, this.treeModel);
        if(selectionMode == Selection.APPENDING) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new AppendingTreeListener(valueField, entityType));
        } else if(selectionMode == Selection.SINGLE) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new SingleTreeListener(valueField, entityType));
        } else if(selectionMode == Selection.FOLLOW) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new FollowTreeListener(valueField, entityType));
        } else if(selectionMode == Selection.FOLLOW_ID) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new IdFollowTreeListener(valueField, entityType));
        } else if (selectionMode == Selection.APPENDING_ID) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new IdAppendingTreeListener(valueField, entityType));
        }
        treePanel.getTree().setRootVisible(!hideRoot);
        getContentPane().add(treePanel, BorderLayout.CENTER);
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
