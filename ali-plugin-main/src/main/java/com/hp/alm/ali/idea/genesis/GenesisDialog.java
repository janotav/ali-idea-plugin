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

package com.hp.alm.ali.idea.genesis;

import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.genesis.checkout.Checkout;
import com.hp.alm.ali.idea.genesis.steps.BranchStep;
import com.hp.alm.ali.idea.genesis.steps.GenesisStep;
import com.hp.alm.ali.idea.genesis.steps.RepositoryStep;
import com.hp.alm.ali.idea.genesis.steps.DomainStep;
import com.hp.alm.ali.idea.genesis.steps.ProjectStep;
import com.hp.alm.ali.idea.genesis.steps.ALMStep;
import com.hp.alm.ali.idea.genesis.steps.ReleaseStep;
import com.hp.alm.ali.idea.genesis.steps.TargetStep;
import com.intellij.ide.wizard.AbstractWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class GenesisDialog extends AbstractWizard<GenesisStep> implements ItemListener, DocumentListener {

    private JPanel panel;
    private WizardContext context;

    public GenesisDialog() {
        super("Provision Development Environment", JOptionPane.getRootFrame());

        AliConfiguration conf = ApplicationManager.getApplication().getComponent(AliConfiguration.class);

        panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout());
        panel3.add(new JLabel(IconLoader.getIcon("/ali_icon_64x64.png")), BorderLayout.NORTH);
        panel.add(panel3, BorderLayout.WEST);

        context = new WizardContext();
        context.panel = panel;

        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayout(0, 1));
        context.locationLbl = new JLabel("HP ALM Location:");
        panel2.add(context.locationLbl);
        context.location = new JTextField(conf.ALM_LOCATION);
        context.location.setPreferredSize(new Dimension(200, context.location.getPreferredSize().height));
        context.location.getDocument().addDocumentListener(this);
        panel2.add(context.location);
        context.usernameLbl = new JLabel("Username:");
        panel2.add(context.usernameLbl);
        context.username = new JTextField(conf.ALM_USERNAME);
        context.username.setPreferredSize(new Dimension(75, context.location.getPreferredSize().height));
        context.username.getDocument().addDocumentListener(this);
        panel2.add(context.username);
        context.passwordLbl = new JLabel("Password:");
        panel2.add(context.passwordLbl);
        context.password = new JPasswordField(conf.ALM_PASSWORD);
        panel2.add(context.password);
        context.domainLbl = new JLabel("Domain:");
        context.domainLbl.setVisible(false);
        panel2.add(context.domainLbl);
        context.domain = new JComboBox();
        context.domain.setVisible(false);
        context.domain.addItemListener(this);
        panel2.add(context.domain);
        context.projectLbl = new JLabel("Project:");
        context.projectLbl.setVisible(false);
        panel2.add(context.projectLbl);
        context.project = new JComboBox();
        context.project.setVisible(false);
        context.project.addItemListener(this);
        panel2.add(context.project);
        context.releaseLbl = new JLabel("Release:");
        context.releaseLbl.setVisible(false);
        panel2.add(context.releaseLbl);
        context.release = new JComboBox();
        context.release.setVisible(false);
        context.release.addItemListener(this);
        panel2.add(context.release);
        context.repositoryLbl = new JLabel("Repository:");
        context.repositoryLbl.setVisible(false);
        panel2.add(context.repositoryLbl);
        context.repository = new JComboBox();
        context.repository.setVisible(false);
        context.repository.addItemListener(this);
        panel2.add(context.repository);
        context.branchLbl = new JLabel("Branch:");
        context.branchLbl.setVisible(false);
        panel2.add(context.branchLbl);
        context.branch = new JComboBox();
        context.branch.setVisible(false);
        context.branch.addItemListener(this);
        panel2.add(context.branch);
        context.targetLbl = new JLabel("Target directory:");
        context.targetLbl.setVisible(false);
        panel2.add(context.targetLbl);
        context.targetBtn = new TextFieldWithBrowseButton(new TextFieldWithBrowseButtonActionListener(context));
        context.targetBtn.setVisible(false);
        panel2.add(context.targetBtn);
        context.targetFullLbl = new JLabel("Checkout as:");
        context.targetFullLbl.setVisible(false);
        panel2.add(context.targetFullLbl);
        context.targetFull = new JComboBox();
        context.targetFull.setVisible(false);
        context.targetFull.addItemListener(this);
        context.targetFull.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                enableDisableNext();
            }
        });
        panel2.add(context.targetFull);

        panel.add(panel2, BorderLayout.CENTER);

        ALMStep qcStep = new ALMStep(context);
        addStep(qcStep);
        DomainStep domainStep = new DomainStep(qcStep, context, conf.ALM_DOMAIN);
        addStep(domainStep);
        ProjectStep projectStep = new ProjectStep(domainStep, context, conf.ALM_PROJECT);
        addStep(projectStep);
        ReleaseStep releaseStep = new ReleaseStep(projectStep, context);
        addStep(releaseStep);
        RepositoryStep repoStep = new RepositoryStep(releaseStep, context);
        addStep(repoStep);
        BranchStep branchStep = new BranchStep(repoStep, context);
        addStep(branchStep);
        TargetStep targetStep = new TargetStep(branchStep, context);
        addStep(targetStep);

        context.targetBtn.getTextField().getDocument().addDocumentListener(targetStep);

        init();

        enableDisableNext();
    }

    private boolean hasDedicatedFinishButton() {
        JButton finishButton = getFinishButton();
        if(finishButton == getFinishButton()) {
            // next and finish are distinct buttons
            return true;
        } else {
            // no dedicated finish button
            return false;
        }
    }

    public JComponent getPreferredFocusedComponent() {
        return context.location;
    }

    public String getTarget() {
        return (String)context.targetFull.getSelectedItem();
    }

    public String getAlmLocation() {
        return context.location.getText();
    }

    public String getDomain() {
        return (String)context.domain.getSelectedItem();
    }

    public String getProject() {
        return (String)context.project.getSelectedItem();
    }

    public String getUsername() {
        return context.username.getText();
    }

    public String getPassword() {
        return context.password.getText();
    }

    public Checkout getCheckout() {
        return context.checkout;
    }

    @Override
    protected String getHelpID() {
        return null;
    }

    public void doNextAction() {
        super.doNextAction();

        if(getCurrentStepObject().isImplicitChoice()) {
            doNextAction();
        }

        if(getCurrentStep() == mySteps.size() - 1 && getCheckout() == null) {
            doOKAction();
        } else {
            getNextButton().setEnabled(getCurrentStepObject().isNextAvailable());
        }
    }

    protected void updateStep() {
        super.updateStep();
        if(hasDedicatedFinishButton() && isLastStep()) {
            getRootPane().setDefaultButton(getFinishButton());
        } else {
            getRootPane().setDefaultButton(getNextButton());
        }
    }

    private void enableDisableNext() {
        if(hasDedicatedFinishButton()) {
            if(isLastStep()) {
                getNextButton().setEnabled(false);
                getFinishButton().setEnabled(getCurrentStepObject().isNextAvailable());
            } else {
                getNextButton().setEnabled(getCurrentStepObject().isNextAvailable());
                getFinishButton().setEnabled(false);
            }
        } else {
            getNextButton().setEnabled(getCurrentStepObject().isNextAvailable());
        }
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        enableDisableNext();
    }

    public void insertUpdate(DocumentEvent documentEvent) {
        enableDisableNext();
    }

    public void removeUpdate(DocumentEvent documentEvent) {
        enableDisableNext();
    }

    public void changedUpdate(DocumentEvent documentEvent) {
        enableDisableNext();
    }

    private static class TextFieldWithBrowseButtonActionListener implements ActionListener {
        private WizardContext ctx;

        public TextFieldWithBrowseButtonActionListener(WizardContext ctx) {
            this.ctx = ctx;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            VirtualFile[] file = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFolderDescriptor(), ctx.targetBtn.getTextField(), null, null);
            if(file.length > 0) {
                ctx.targetBtn.getTextField().setText(file[0].getPath());
            }
        }
    }
}
