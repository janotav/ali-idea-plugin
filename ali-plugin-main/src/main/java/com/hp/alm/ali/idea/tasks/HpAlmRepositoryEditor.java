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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.content.settings.SettingsPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HpAlmRepositoryEditor extends TaskRepositoryEditor implements ItemListener {
    private Project project;
    private HpAlmRepository repository;

    private static Map<JPanel, HpAlmRepository> allInstances = new HashMap<JPanel, HpAlmRepository>();

    private JLabel defectOrRequirementWarning;
    private JPanel options;

    public HpAlmRepositoryEditor(Project project, HpAlmRepository repository) {
        this.project = project;
        this.repository = repository;

        repository.setUrl(project.getName());
        repository.setId(System.currentTimeMillis());
    }

    public JComponent createComponent() {
        final JPanel content = new JPanel(new BorderLayout());

        Icon icon = IconLoader.getIcon("/general/warningDialog.png");
        defectOrRequirementWarning = new JLabel("<html><body>No tasks will be returned when neither defects nor requirements are selected</body></html>", icon, SwingConstants.LEFT);

        options = new JPanel();
        options.setLayout(new GridBagLayout());
        options.setBorder(new EmptyBorder(0, 10, 0, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 0;

        options.add(new SettingsPanel(project, content.getBackground()) {
            // avoid horizontal scrollbar because of this panel
            public Dimension getPreferredSize() {
                return new Dimension(450, 200);
            }

        }, c);
        c.gridy++;

        options.add(new JLabel("When querying tasks consider following entities:"), c);
        c.gridy++;

        options.add(new TaskConfigPanel(project, "Defects", repository.getDefect(), "defect", this), c);
        c.gridy++;

        options.add(new TaskConfigPanel(project, "Requirements", repository.getRequirement(), "requirement", this), c);
        c.gridy++;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1.0;
        c.weightx = 1.0;
        options.add(new JPanel(), c);

        content.add(options, BorderLayout.CENTER);

        JLabel warning = new JLabel("<html><body>Integration with HP ALM repository can be defined only once per project.</body></html>", IconLoader.getIcon("/general/errorDialog.png"), SwingConstants.LEFT);

        final CardLayout cardLayout = new CardLayout();
        final JPanel contentOrWarning = new JPanel(cardLayout);
        contentOrWarning.add(new JBScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), "content");
        contentOrWarning.add(warning, "warning");
        cardLayout.show(contentOrWarning, "content");

        allInstances.put(contentOrWarning, repository);

        contentOrWarning.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (propertyChangeEvent.getOldValue() == null && propertyChangeEvent.getNewValue() != null) {
                    disableDuplicatedEntries();
                    installRemoveEditorWorkaround(content.getParent().getParent());
                } else if (propertyChangeEvent.getOldValue() != null && propertyChangeEvent.getNewValue() == null) {
                    // this event won't be triggered unless workaround succeeded
                    allInstances.remove(contentOrWarning);
                    disableDuplicatedEntries();
                }
            }
        });

        evaluateDefectOrRequirementWarning();

        return contentOrWarning;
    }

    private void disableDuplicatedEntries() {
        if(!allInstances.isEmpty()) {
            ArrayList<HpAlmRepository> values = new ArrayList<HpAlmRepository>(allInstances.values());
            Collections.sort(values);
            HpAlmRepository first = values.get(0);
            for (JPanel panel: allInstances.keySet()) {
                if(allInstances.get(panel) == first) {
                    ((CardLayout)panel.getLayout()).show(panel, "content");
                } else {
                    ((CardLayout)panel.getLayout()).show(panel, "warning");
                }
            }
        }
    }

    private void evaluateDefectOrRequirementWarning() {
        if(!repository.getDefect().isEnabled() && !repository.getRequirement().isEnabled()) {
            if(defectOrRequirementWarning.getParent() == null) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 5;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.WEST;
                options.add(defectOrRequirementWarning, c);
                options.revalidate();
                options.repaint();
            }
        } else {
            if(defectOrRequirementWarning.getParent() != null) {
                options.remove(defectOrRequirementWarning);
                options.revalidate();
                options.repaint();
            }
        }
    }

    private void installRemoveEditorWorkaround(final Container contentOrWarning) {
        try {
            final ListModel model = ((JBList) ((JBScrollPane) contentOrWarning.getParent().getParent().getComponent(1)).getViewport().getComponent(0)).getModel();
            model.addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent listDataEvent) {
                    recalculate();
                }

                public void intervalRemoved(ListDataEvent listDataEvent) {
                    recalculate();
                }

                public void contentsChanged(ListDataEvent listDataEvent) {
                    recalculate();
                }

                private void recalculate() {
                    try {
                        if(contentOrWarning.getParent() != null) {
                            for(int i = 0; i < model.getSize(); i++) {
                                if(repository.equals(model.getElementAt(i))) {
                                    return;
                                }
                            }
                            // repository is no longer listed
                            contentOrWarning.getParent().remove(contentOrWarning);
                        }
                    } catch(Exception e) {
                        // workaround failed, the warning dialog icon won't disappear until server configuration is reinitialized
                    }
                }
            });
        } catch(Exception e) {
            // workaround failed, the warning dialog icon won't disappear until server configuration is reinitialized
        }
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        evaluateDefectOrRequirementWarning();
    }
}
