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

import com.hp.alm.ali.idea.cfg.TaskBoardConfiguration;
import com.hp.alm.ali.idea.ui.BoxablePanel;
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.intellij.openapi.project.Project;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;

public class TaskBoardSettingsDialog extends MyDialog {

    private TaskBoardConfiguration configuration;
    private JCheckBox taskCompleteSwitch;
    private JComboBox<String> itemStatuses;
    private JCheckBox deactivateItemSwitch;
    private JCheckBox assignTaskSwitch;
    private JCheckBox activateItemSwitch;

    public TaskBoardSettingsDialog(Project project) {
        super(project, JOptionPane.getRootFrame(), "Task Board Settings", true, true, Arrays.asList(Button.OK, Button.Cancel));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

        JPanel taskComplete = new BoxablePanel();
        taskComplete.setAlignmentX(Component.LEFT_ALIGNMENT);
        taskComplete.setBorder(BorderFactory.createTitledBorder("When last task is completed"));
        taskComplete.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        taskCompleteSwitch = new JCheckBox("Move backlog item to ");
        taskComplete.add(taskCompleteSwitch, c);
        itemStatuses = new JComboBox<String>(new String[]{
                BacklogItemPanel.ITEM_IN_TESTING,
                BacklogItemPanel.ITEM_DONE
        });
        c.gridx++;
        taskComplete.add(itemStatuses, c);
        taskCompleteSwitch.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                itemStatuses.setEnabled(taskCompleteSwitch.isSelected());
            }
        });
        deactivateItemSwitch = new JCheckBox("Deactivate work item if active");
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        taskComplete.add(deactivateItemSwitch, c);
        contentPanel.add(taskComplete);

        JPanel assignTask = new BoxablePanel(new FlowLayout());
        assignTask.setAlignmentX(Component.LEFT_ALIGNMENT);
        assignTask.setBorder(BorderFactory.createTitledBorder("When starting work on task"));
        assignTaskSwitch = new JCheckBox("Assign it to me");
        assignTask.add(assignTaskSwitch);
        activateItemSwitch = new JCheckBox("Activate work item");
        assignTask.add(activateItemSwitch);
        contentPanel.add(assignTask);

        configuration = project.getComponent(TaskBoardConfiguration.class);
        String status = configuration.getTasksCompletedStatus();
        if (status == null) {
            itemStatuses.setEnabled(false);
        } else {
            taskCompleteSwitch.setSelected(true);
            itemStatuses.setSelectedItem(status);
        }
        assignTaskSwitch.setSelected(configuration.isAssignTask());
        deactivateItemSwitch.setSelected(configuration.isDeactivateItem());
        activateItemSwitch.setSelected(configuration.isActivateItem());

        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getRootPane().setDefaultButton(getButton(Button.OK));

        pack();
        setLocationRelativeTo(null);
    }

    protected void buttonPerformed(Button button) {
        super.buttonPerformed(button);

        switch (button) {
            case OK:
                if (taskCompleteSwitch.isSelected()) {
                    configuration.setTasksCompletedStatus((String) itemStatuses.getSelectedItem());
                } else {
                    configuration.setTasksCompletedStatus(null);
                }
                configuration.setAssignTask(assignTaskSwitch.isSelected());
                configuration.setDeactivateItem(deactivateItemSwitch.isSelected());
                configuration.setActivateItem(activateItemSwitch.isSelected());
                close(true);
                break;
        }
    }
}
