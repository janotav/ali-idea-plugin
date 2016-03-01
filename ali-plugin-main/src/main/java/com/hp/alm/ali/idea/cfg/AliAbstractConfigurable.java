/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.cfg;

import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.util.ApplicationUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

public abstract class AliAbstractConfigurable implements SearchableConfigurable, DocumentListener {

    public static final String HP_ALM_INTEGRATION = "HPE ALM Integration Configuration";

    protected ConfigurationField locationField;
    protected ConfigurationField domainField;
    protected ConfigurationField projectField;
    protected ConfigurationField usernameField;
    protected ConfigurationField passwdField;
    protected JCheckBox storePasswd;

    private JButton testButton = new JButton("Test", IconLoader.getIcon("/toolwindows/toolWindowRun.png"));
    private JLabel testLabel = new JLabel("");

    protected JPanel configurationPanel;

    public Runnable enableSearch(String option) {
        return null;
    }

    public void disposeUIResources() {
    }

    public abstract String getDisplayName();

    public Icon getIcon() {
        return null;
    }

    public String getHelpTopic() {
        return null;
    }

    protected abstract void loadConfiguration();

    protected abstract String getCaption();

    protected abstract ConfigurationField getLocationField();

    protected abstract ConfigurationField getUsernameField();

    protected abstract ConfigurationField getPasswordField();

    protected abstract ConfigurationField getDomainField();

    protected abstract ConfigurationField getProjectField();

    protected void addAdditionalSettings(JPanel panel, GridBagConstraints c) {
    }

    protected Component getSouthernComponent() {
        return null;
    }

    protected void onConfigurationPanelInitialized() {
    }

    public void insertUpdate(DocumentEvent documentEvent) {
        enableDisableTest();
    }

    public void removeUpdate(DocumentEvent documentEvent) {
        enableDisableTest();
    }

    public void changedUpdate(DocumentEvent documentEvent) {
        enableDisableTest();
    }

    public JComponent createComponent() {
        ensureConfiguration();
        return configurationPanel;
    }

    protected void ensureConfiguration() {
        if(configurationPanel == null) {
            loadConfiguration();
            initialize();
            onConfigurationPanelInitialized();
        }
    }

    public boolean isModified(String location, String domain, String project, String username, String password,
                              boolean storePassword) {
        return !locationField.getText().equals(location) ||
                !domainField.getText().equals(domain) ||
                !projectField.getText().equals(project) ||
                !usernameField.getText().equals(username) ||
                !passwdField.getText().equals(password) ||
                (storePasswd.isSelected() && storePasswd.isEnabled()) != storePassword;
    }

    protected void enableDisableTest() {
        testLabel.setText("");
        testButton.setEnabled(!domainField.getText().isEmpty() && !projectField.getText().isEmpty() && !locationField.getText().isEmpty() && !usernameField.getText().isEmpty());
        storePasswd.setEnabled(!passwdField.getValue().isEmpty());
    }

    private void setTestText(final String value) {
        ApplicationUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                testLabel.setText(value);
            }
        });
    }

    private void initialize() {
        SearchableOptionsRegistrar.getInstance().addOption("integration", null, HP_ALM_INTEGRATION,getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("alm", null, HP_ALM_INTEGRATION, getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("qc", null, HP_ALM_INTEGRATION, getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("QC", null, HP_ALM_INTEGRATION, getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("agm", null, HP_ALM_INTEGRATION,getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("agile", null, HP_ALM_INTEGRATION, getId(), getDisplayName());
        SearchableOptionsRegistrar.getInstance().addOption("manager", null, HP_ALM_INTEGRATION, getId(), getDisplayName());

        JPanel content = new JPanel(new BorderLayout());
        JLabel label = new JLabel(IconLoader.getIcon("/ali_icon_64x64.png"));
        label.setVerticalAlignment(SwingConstants.TOP);

        JPanel jPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 6;
        jPanel.add(label, c);
        c.gridheight = 1;
        c.gridx++;
        c.gridwidth = 3;
        jPanel.add(new JLabel(getCaption()), c);
        c.gridwidth = 1;
        c.gridy++;
        jPanel.add(new JLabel("Location:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("location", null, "Location:", getId(), getDisplayName());
        c.gridx++;
        locationField = getLocationField();
        jPanel.add((JTextComponent)locationField, c);
        locationField.getDocument().addDocumentListener(this);
        c.gridx = 2;
        c.gridy++;
        c.gridwidth = 2;
        JLabel exLabel = new JLabel("E.g. http://mycompany.com:8080/qcbin");
        exLabel.setFont(exLabel.getFont().deriveFont(Font.ITALIC, exLabel.getFont().getSize() - 2));
        jPanel.add(exLabel, c);
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Username:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("username", null, "Username:", getId(), getDisplayName());
        c.gridx++;
        usernameField = getUsernameField();
        jPanel.add((JTextComponent)usernameField, c);
        usernameField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Password:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("password", null, "Password:", getId(), getDisplayName());
        c.gridx++;
        passwdField = getPasswordField();
        jPanel.add((JTextComponent)passwdField, c);
        passwdField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Domain:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("domain", null, "Domain:", getId(), getDisplayName());
        c.gridx++;
        domainField = getDomainField();
        jPanel.add((JTextComponent)domainField, c);
        domainField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Project:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("project", null, "Project:", getId(), getDisplayName());
        c.gridx++;
        projectField = getProjectField();
        jPanel.add((JTextComponent)projectField, c);
        projectField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 2;
        storePasswd = new JCheckBox("Remember password");
        jPanel.add(storePasswd, c);
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 1;
        testButton.setDisabledIcon(IconLoader.getIcon("/process/disabledRun.png"));
        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ProgressManager.getInstance().run(new Task.Modal(null, "Checking connection", false) {
                    public void run(ProgressIndicator indicator) {
                        try {
                            indicator.setIndeterminate(true);
                            ServerType type = AliConfigurable.getServerType(locationField.getText().trim(), domainField.getText().trim(), projectField.getText().trim(), usernameField.getText().trim(), passwdField.getText());
                            switch (type) {
                                case ALM11:
                                case ALI:
                                case ALI2:
                                case ALM11_5:
                                case ALI11_5:
                                case ALM12:
                                case ALI12:
                                case AGM:
                                    setTestText("Connection successful (" + type.toString() + ")");
                                    break;
                            }
                        } catch(Exception e) {
                            setTestText(e.getMessage());
                        }
                    }
                });
            }
        });
        jPanel.add(testButton, c);
        c.gridx++;
        c.gridwidth = 2;
        jPanel.add(testLabel, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        jPanel.add(new JLabel(IconLoader.getIcon("/horizon.png")), c);

        JTextPane desc = HTMLAreaField.createTextPane("<html>When connecting to HPE Agile Manager you can either fill in the above form<br>" +
                "manually or upload the tenant descriptor to auto-configure the values.<br>" +
                "You can obtain the tenant descriptor from the ALI Summary tab in the<br>" +
                "HPE Agile Manager configuration</html>");
        desc.setOpaque(false);
        c.gridwidth = 3;
        c.gridx++;
        jPanel.add(desc, c);

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        final JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor desc = new FileChooserDescriptor(true, false, false, false, false, false);
                final VirtualFile[] file = FileChooserFactory.getInstance().createFileChooser(desc, null, browse).choose((VirtualFile)null, null);
                if(file.length == 0) {
                    return;
                }

                try {
                    SAXBuilder builder = new SAXBuilder();
                    InputStream fis = file[0].getInputStream();
                    Document document = builder.build(fis);
                    Element rootNode = document.getRootElement();
                    locationField.setValue(rootNode.getChild("location").getText());
                    domainField.setValue(rootNode.getChild("domain").getText());
                    projectField.setValue(rootNode.getChild("project").getText());
                    fis.close();
                } catch (Exception ex) {
                    Messages.showErrorDialog("Tenant descriptor is not valid.", "Error");
                }
            }
        });
        jPanel.add(browse, c);

        addAdditionalSettings(jPanel, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy++;
        c.weightx = 1.0;
        c.weighty = 1.0;
        jPanel.add(new JPanel(), c);

        content.add(jPanel, BorderLayout.CENTER);
        Component southern = getSouthernComponent();
        if(southern != null) {
            content.add(southern, BorderLayout.SOUTH);
        }

        configurationPanel = content;
    }
}
