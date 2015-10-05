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

import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class TimePanel extends JPanel {

    JLabel effortLabel;
    private JLabel investedLabel;
    private JLabel investedEstimatedSeparator;
    private JLabel remainingLabel;

    public TimePanel(Project project, TaskPanel pTaskPanel) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 5));

        setOpaque(false);

        effortLabel = new JLabel(IconLoader.getIcon("/general/secondaryGroup.png"));
        add(effortLabel);
        investedLabel = new JLabel();
        add(investedLabel);
        investedEstimatedSeparator = new JLabel("|");
        add(investedEstimatedSeparator);
        remainingLabel = new JLabel();
        add(remainingLabel);

        update(pTaskPanel.getTask());
    }

    public void update(Entity task) {
        String remaining = task.getPropertyValue("remaining");
        String invested = task.getPropertyValue("invested");
        String status = task.getPropertyValue("status");

        investedLabel.setText(invested);
        remainingLabel.setText(remaining);

        if(TaskPanel.TASK_IN_PROGRESS.equals(status)) {
            effortLabel.setVisible(true);
            investedLabel.setVisible(true);
            investedEstimatedSeparator.setVisible(true);
            remainingLabel.setVisible(true);
        } else if(TaskPanel.TASK_NEW.equals(status)) {
            effortLabel.setVisible(false);
            investedLabel.setVisible(false);
            investedEstimatedSeparator.setVisible(false);
            remainingLabel.setVisible(false);
        } else {
            effortLabel.setVisible(true);
            investedLabel.setVisible(true);
            investedEstimatedSeparator.setVisible(false);
            remainingLabel.setVisible(false);
        }
    }
}
