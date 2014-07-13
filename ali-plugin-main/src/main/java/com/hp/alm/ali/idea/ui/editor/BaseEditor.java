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

package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.GridBagSplitter;
import com.hp.alm.ali.idea.ui.ScrollablePanel;
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.ui.editor.field.EditableField;
import com.hp.alm.ali.idea.ui.editor.field.EditableFieldListener;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseEditor extends MyDialog {

    private GridBagConstraints gbc;
    protected JPanel gridPanel;
    protected JPanel gridFooter;

    protected Map<String, EditableField> fields;
    private int verticalExpandCount = 0;

    private JButton save;

    protected Project project;
    protected Entity entity;
    protected SaveHandler saveHandler;

    public BaseEditor(Project project, String title, Entity entity, SaveHandler saveHandler) {
        super(project, JOptionPane.getRootFrame(), title, false, true, Arrays.asList(Button.Save, Button.Cancel));
        this.project = project;
        this.entity = entity;
        this.saveHandler = saveHandler;

        fields = new LinkedHashMap<String, EditableField>();

        gridPanel = new ScrollablePanel(new GridBagLayout()) {
            public Dimension getPreferredSize() {
                // allocate room for expandable content
                Dimension dim = super.getPreferredSize();
                dim.height += verticalExpandCount * 100;
                return dim;
            }
        };
        gridFooter = new JPanel();

        JPanel panel = new ScrollablePanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(gridPanel, BorderLayout.CENTER);
        panel.add(gridFooter, BorderLayout.SOUTH);
        getContentPane().add(new JBScrollPane(panel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

        save = getButton(Button.Save);
        save.setEnabled(false);
        getRootPane().setDefaultButton(save);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    public void execute() {
        final EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                final Entity locked = entityEditManager.startEditing(entity);
                if(locked != null) {
                    BaseEditor.this.entity = locked;

                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        @Override
                        public void run() {
                            // reset and update UI
                            fields.clear();
                            gridPanel.removeAll();
                            gbc = new GridBagConstraints();
                            gbc.gridx = 0;
                            gbc.gridy = 0;
                            gbc.insets = new Insets(5, 5, 5, 5);
                            gbc.anchor = GridBagConstraints.NORTH;
                            gbc.fill = GridBagConstraints.BOTH;
                            update();

                            save.addPropertyChangeListener("enabled", new PropertyChangeListener() {
                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {
                                    entityEditManager.setEntityDirty(locked, Boolean.TRUE.equals(evt.getNewValue()));
                                }
                            });
                            addWindowListener(new WindowAdapter() {
                                @Override
                                public void windowClosed(WindowEvent e) {
                                    if (!project.isDisposed()) {
                                        entityEditManager.stopEditing(locked);
                                    }
                                }

                                @Override
                                public void windowClosing(WindowEvent e) {
                                    close(false);
                                }
                            });
                            setVisible(true);
                        }
                    });
                }
            }
        });
    }

    public Entity getModified() {
        Entity ret = new Entity(entity.getType(), entity.getId());
        for(String column: fields.keySet()) {
            EditableField ef = fields.get(column);
            if(entity.getId() <= 0 || ef.hasChanged()) {
                ret.setProperty(column, ef.getValue());
            }
        }
        return ret;
    }

    protected void buttonPerformed(Button button) {
        super.buttonPerformed(button);

        switch (button) {
            case Save:
                StringBuffer message = new StringBuffer();
                String error = validate(message);
                if(error != null) {
                    Messages.showErrorDialog(message.toString(), error);
                    return;
                }
                if(saveHandler.save(getModified(), entity)) {
                    close(true);
                }
        }
    }

    protected void packAndPosition() {
        pack();
        setLocationRelativeTo(null);
    }

    private void updateSave() {
        for(EditableField field: fields.values()) {
            if(field.isRequired() && field.getValue().isEmpty()) {
                save.setEnabled(false);
                return;
            }
        }
        for(EditableField field: fields.values()) {
            if(field.hasChanged()) {
                save.setEnabled(true);
                return;
            }
        }
        save.setEnabled(false);
    }

    public String validate(StringBuffer message) {
        return null;
    }

    public abstract void update();

    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.width = Math.min(dim.width, 800);
        dim.height = Math.min(dim.height, 600);
        return dim;
    }

    public void addField(String name, EditableField field) {
        addField(name, field, false);
    }

    public void addField(String name, EditableField field, boolean verticalExpand) {
        fields.put(name, field);

        final JLabel label = makeLabel(field.getLabel());
        gbc.weightx = 0;
        gbc.weighty = 0;

        if(verticalExpand) {
            if(verticalExpandCount++ > 0) {
                // at least two expandable fields: allow to reallocate space
                GridBagSplitter gridBagSplitter = new GridBagSplitter(this, gridPanel, gbc.gridy) {
                    protected Component getComponentForRow(int n) {
                        return gridPanel.getComponent(n * 2 + 1);
                    }
                };

                gbc.insets = new Insets(0, 0, 0, 0);
                // simplify mapping in gridBagSplitter by inserting dummy component
                gridPanel.add(new JPanel(), gbc);
                gbc.gridx++;
                gridPanel.add(gridBagSplitter.getComponent(), gbc);
                gbc.gridy++;
                gbc.gridx = 0;
                gbc.insets = new Insets(5, 5, 5, 5);
            }
        }

        gridPanel.add(label, gbc);
        gbc.gridx++;

        Component comp = field.getComponent();
        gbc.weightx = 1;
        gbc.weighty = verticalExpand? 1: 0;
        gridPanel.add(comp, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        if(field.isDisableDefaultAction()) {
            comp.addFocusListener(new FocusListener() {
                JButton defaultButton;
                public void focusGained(FocusEvent focusEvent) {
                    defaultButton = getRootPane().getDefaultButton();
                    getRootPane().setDefaultButton(null);
                }

                public void focusLost(FocusEvent focusEvent) {
                    getRootPane().setDefaultButton(defaultButton);
                }
            });
        }

        field.addUpdateListener(new EditableFieldListener() {
            public void updated(EditableField field) {
                makeBold(label, field.hasChanged());
                if(field.isRequired() && field.getValue().isEmpty()) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(UIManager.getDefaults().getColor("Label.foreground"));
                }
                updateSave();
            }
        });

        if(field.isRequired() && field.getValue().isEmpty()) {
            label.setForeground(Color.RED);
        }
    }

    public EditableField getField(String name) {
        return fields.get(name);
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        // make room for bold text (avoid re-layout)
        Dimension dim = label.getPreferredSize();
        dim.width = (int) (dim.width * 1.2f);
        label.setPreferredSize(dim);
        label.setMinimumSize(dim);
        label.setVerticalAlignment(JLabel.TOP);
        return label;
    }

    public static boolean makeBold(JLabel label, boolean bold) {
        Font f = label.getFont();
        if(bold) {
            label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        } else {
            label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
        }
        return bold;
    }

    public static interface SaveHandler {

        boolean save(Entity modified, Entity base);

    }

    public static abstract class BaseHandler implements SaveHandler {

        protected EntityService entityService;

        public BaseHandler(Project project) {
            entityService = project.getComponent(EntityService.class);
        }
    }
}
