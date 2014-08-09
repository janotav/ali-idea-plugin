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

package com.hp.alm.ali.idea.ui.dialog;

import com.hp.alm.ali.idea.model.Audit;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.ui.AuditFilterPanel;
import com.hp.alm.ali.idea.model.AuditModel;
import com.hp.alm.ali.idea.services.ProjectUserService;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.model.parser.AuditList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HistoryDialog extends MyDialog {

    private Project project;
    private JPanel outer;
    private JPanel contentPanel;

    public HistoryDialog(final Project project, final Entity entity) {
        super(project, JOptionPane.getRootFrame(), "History...", false, true, Arrays.asList(Button.Close));

        this.project = project;

        setEditorTitle(project, "History of {0} #" + entity.getId(), entity.getType());

        contentPanel = new JPanel();
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(new JLabel("Loading history information..."));

        outer = new JPanel(new BorderLayout());
        outer.setBorder(new EmptyBorder(10, 10, 10, 10));
        outer.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
        getContentPane().add(outer, BorderLayout.CENTER);

        getRootPane().setDefaultButton(getButton(Button.Close));

        pack();
        setLocationRelativeTo(null);

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                final AuditList auditList = project.getComponent(RestService.class).getServerStrategy().getEntityAudit(entity);

                outer.add(new AuditFilterPanel(project, auditList, new AuditFilterPanel.Listener() {
                    public void changed(Set<String> selectedFields, Set<String> selectedUsers) {
                        showHistory(auditList, selectedFields, selectedUsers);
                        outer.revalidate();
                        outer.repaint();
                    }
                }), BorderLayout.NORTH);

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    public void run() {
                        showHistory(auditList, Collections.<String>emptySet(), Collections.<String>emptySet());
                        contentPanel.requestFocus();
                        packAndCenter(800, 600, true);
                    }
                });
            }
        });
    }

    private void showHistory(List<Audit> audits, Set<String> selectedFields, Set<String> selectedUsers) {
        contentPanel.removeAll();
        if(audits.isEmpty()) {
            contentPanel.add(new JLabel("No history information available"));
        } else {
            for (int i = 0; i < audits.size(); i++) {
                Component comp = createAudit(audits.get(i), audits.size() - i, selectedFields, selectedUsers);
                if(comp != null) {
                    contentPanel.add(comp);
                }
            }
        }
    }

    private Component createAudit(Audit audit, int rev, Set<String> selectedFields, Set<String> selectedUsers) {
        String userName = audit.getUsername();
        if(!selectedUsers.isEmpty() && !selectedUsers.contains(userName)) {
            return null;
        }

        List<String[]> properties = audit.getProperties();
        if(!selectedFields.isEmpty()) {
            List<String[]> myProperties = new ArrayList<String[]>();
            for (String[] property : properties) {
                if (selectedFields.contains(property[0])) {
                    myProperties.add(property);
                }
            }
            properties = myProperties;
        }
        if(properties.isEmpty()) {
            return null;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(5, 0, 5, 0));

        String fullName = project.getComponent(ProjectUserService.class).getUserFullName(userName);

        StringBuffer buf = new StringBuffer();
        buf.append("By ");
        if(fullName != null && !fullName.isEmpty()) {
            buf.append(fullName).append(" ");
        }
        buf.append("<").append(userName).append(">");
        buf.append(" at ");
        buf.append(CommentField.dateTimeFormat.format(audit.getDate()));
        buf.append(" ").append("(revision ").append(rev).append(")");

        JLabel label = new JLabel(buf.toString(), JLabel.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(12, 1, 5, 2));
        label.setFont(label.getFont().deriveFont((float) label.getFont().getSize() + 2));
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        JComponent table = createTable(properties);
        table.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(table);

        return panel;
    }

    private JComponent createTable(List<String[]> properties) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JBTable table = new JBTable(new AuditModel(properties));
        table.setDefaultRenderer(String.class, new MyTableCellRenderer());
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(table.getGridColor().brighter());
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new MyHeaderCellRenderer());
        panel.add(table.getTableHeader());
        panel.add(table);
        return panel;
    }

    public static class MyHeaderCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            JLabel renderer = (JLabel)super.getTableCellRendererComponent(table, value, false, false, row, column);
            renderer.setHorizontalAlignment(JLabel.LEFT);
            renderer.setBorder(BorderFactory.createEtchedBorder());
            return renderer;
        }
    }


    public static class MyTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            return super.getTableCellRendererComponent(table, value, false, false, row, column);
        }
    }
}
