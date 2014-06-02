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
import com.hp.alm.ali.idea.ui.dialog.MyDialog;
import com.intellij.openapi.project.Project;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;

public class TaskBoardSettingsDialog extends MyDialog {

    private TaskBoardConfiguration configuration;
    private JCheckBox taskCompleteSwitch;
    private JComboBox<String> itemStatuses;

    public TaskBoardSettingsDialog(Project project) {
        super(project, JOptionPane.getRootFrame(), "Task Board Settings", true, true, Arrays.asList(Button.OK, Button.Cancel));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel taskComplete = new JPanel(new FlowLayout());
        taskCompleteSwitch = new JCheckBox("When last task is completed, move backlog item to ");
        taskComplete.add(taskCompleteSwitch);
        itemStatuses = new JComboBox<String>(new String[]{
                BacklogItemPanel.ITEM_IN_TESTING,
                BacklogItemPanel.ITEM_DONE
        });
        taskComplete.add(itemStatuses);
        taskCompleteSwitch.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                itemStatuses.setEnabled(taskCompleteSwitch.isSelected());
            }
        });
        contentPanel.add(taskComplete);

        configuration = project.getComponent(TaskBoardConfiguration.class);
        String status = configuration.getTasksCompletedStatus();
        if (status == null) {
            itemStatuses.setEnabled(false);
        } else {
            taskCompleteSwitch.setSelected(true);
            itemStatuses.setSelectedItem(status);
        }

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
                close(true);
                break;
        }
    }
}
