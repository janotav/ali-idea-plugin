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

package com.hp.alm.ali.idea.content.taskboard;

import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.ui.ActiveItemLink;
import com.hp.alm.ali.idea.content.AliContent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class TaskBoardContent implements AliContent {

    private static TaskBoardContent instance = new TaskBoardContent();

    public static TaskBoardContent getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Task Board";
    }

    @Override
    public JComponent create(Project project) {
        JPanel panel = new JPanel(new BorderLayout());
        final TaskBoardPanel taskBoardPanel = new TaskBoardPanel(project);

        panel.add(taskBoardPanel.getHeader(), BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(taskBoardPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(TaskPanel.SIZE.height);
        scrollPane.setColumnHeaderView(taskBoardPanel.getColumnHeader());
        panel.add(scrollPane, BorderLayout.CENTER);

        JComponent toolbar = ActionUtil.createActionToolbar("hpali.taskboard", TaskBoardPanel.PLACE, true, false).getComponent();
        panel.add(toolbar, BorderLayout.WEST);

        JPanel statusBar = new JPanel();
        statusBar.setLayout(new BorderLayout());
        statusBar.add(taskBoardPanel.getStatusComponent(), BorderLayout.WEST);
        statusBar.add(new ActiveItemLink(project), BorderLayout.EAST);
        panel.add(statusBar, BorderLayout.SOUTH);

        return panel;
    }
}
