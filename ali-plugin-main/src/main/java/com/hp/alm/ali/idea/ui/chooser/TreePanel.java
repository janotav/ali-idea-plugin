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

import com.hp.alm.ali.idea.entity.tree.HierarchicalEntityModel;
import com.hp.alm.ali.idea.ui.entity.EntityStatusPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class TreePanel extends JPanel {

    private static Icon findIcon = IconLoader.getIcon("/actions/find.png");

    // we don't use "search as you type" to be consistent with entity table (see comment there)
    // in our case however the only issue that would need to be solved is the delay mechanism,
    // because wildcards are used implicitly

    private JTextField filter;
    private FilterableTree tree;

    public TreePanel(Project project, HierarchicalEntityModel treeModel) {
        super(new BorderLayout());

        JPanel filerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filter = new JTextField(16);
        filter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                filterChanged();
            }
        });
        filerPanel.add(filter);
        LinkLabel filterIcon = new LinkLabel("", findIcon);
        filterIcon.setListener(new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object o) {
                filterChanged();
            }
        }, null);
        filterIcon.setBorder(new EmptyBorder(0, 2, 0, 0));
        filerPanel.add(filterIcon);
        add(filerPanel, BorderLayout.NORTH);

        tree = new FilterableTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new ALMTreeCellRenderer());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(tree), BorderLayout.CENTER);
        EntityStatusPanel status = new EntityStatusPanel(project);
        treeModel.setStatus(status);
        status.setBorder(BorderFactory.createEtchedBorder());
        panel.add(status, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void filterChanged() {
        tree.setFilter(filter.getText());
    }

    public FilterableTree getTree() {
        return tree;
    }
}
