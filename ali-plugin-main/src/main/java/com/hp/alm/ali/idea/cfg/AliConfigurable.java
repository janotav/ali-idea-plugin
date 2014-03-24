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

package com.hp.alm.ali.idea.cfg;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.parser.ProjectExtensionsList;
import com.hp.alm.ali.idea.rest.RestException;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.TroubleShootService;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.rest.client.exception.HttpClientErrorException;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.apache.commons.httpclient.HttpStatus;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;

public class AliConfigurable implements SearchableConfigurable, DocumentListener {

    public static final String NAME = "HP ALI";
    public static final String HP_ALM_INTEGRATION = "HP ALM Integration Configuration";
    private AliConfiguration aliConfiguration;

    protected ConfigurationField locationField;
    protected ConfigurationField domainField;
    protected ConfigurationField projectField;
    protected ConfigurationField usernameField;
    protected ConfigurationField passwdField;
    protected JCheckBox storePasswd;

    private JButton testButton = new JButton("Test", IconLoader.getIcon("/toolwindows/toolWindowRun.png"));
    private JLabel testLabel = new JLabel("");

    protected JPanel configurationPanel;

    protected void initialize() {
        SearchableOptionsRegistrar.getInstance().addOption("integration", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("alm", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("qc", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("QC", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("agm", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("agile", null, HP_ALM_INTEGRATION, "HP", NAME);
        SearchableOptionsRegistrar.getInstance().addOption("manager", null, HP_ALM_INTEGRATION, "HP", NAME);

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
        SearchableOptionsRegistrar.getInstance().addOption("location", null, "Location:", "HP", NAME);
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
        SearchableOptionsRegistrar.getInstance().addOption("username", null, "Username:", "HP", NAME);
        c.gridx++;
        usernameField = getUsernameField();
        jPanel.add((JTextComponent)usernameField, c);
        usernameField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Password:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("password", null, "Password:", "HP", NAME);
        c.gridx++;
        passwdField = getPasswordField();
        jPanel.add((JTextComponent)passwdField, c);
        passwdField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Domain:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("domain", null, "Domain:", "HP", NAME);
        c.gridx++;
        domainField = getDomainField();
        jPanel.add((JTextComponent)domainField, c);
        domainField.getDocument().addDocumentListener(this);
        c.gridx = 1;
        c.gridy++;
        jPanel.add(new JLabel("Project:"), c);
        SearchableOptionsRegistrar.getInstance().addOption("project", null, "Project:", "HP", NAME);
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
                            ServerType type = getServerType(locationField.getText().trim(), domainField.getText().trim(), projectField.getText().trim(), usernameField.getText().trim(), passwdField.getText());
                            switch (type) {
                                case ALM11:
                                case ALI:
                                case ALI2:
                                case ALM11_5:
                                case ALM12:
                                case AGM:
                                    testLabel.setText("Connection successful (" + type.toString() + ")");
                                    break;
                            }
                        } catch(Exception e) {
                            testLabel.setText(e.getMessage());
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

        JTextPane desc = HTMLAreaField.createTextPane("<html>When connecting to HP Agile Manager you can either fill in the above form<br>" +
                            "manually or upload the tenant descriptor to auto-configure the values.<br>" +
                            "You can obtain the tenant descriptor from the ALI Summary tab in the<br>"+
                            "HP Agile Manager configuration</html>");
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
                final VirtualFile[] file = FileChooserFactory.getInstance().createFileChooser(desc, null, browse).choose(null, null);
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

    protected Component getSouthernComponent() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final TroubleShootService troubleShootService = ApplicationManager.getApplication().getComponent(TroubleShootService.class);
        final JButton troubleshoot = new JButton(troubleShootService.isRunning()? "Stop Troubleshoot": "Troubleshoot");
        troubleshoot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(troubleshoot.getText().equals("Troubleshoot")) {
                    if(!troubleShootService.isRunning()) {
                        if(Messages.showYesNoDialog("Do you want to log complete ALM server communication?", "Confirmation", null) == Messages.YES) {
                            FileSaverDescriptor desc = new FileSaverDescriptor("Log server communication", "Log server communication on the local filesystem.");
                            final VirtualFileWrapper file = FileChooserFactory.getInstance().createSaveFileDialog(desc, troubleshoot).save(null, "REST_log.txt");
                            if(file == null) {
                                return;
                            }

                            troubleShootService.start(file.getFile());
                            troubleshoot.setText("Stop Troubleshoot");
                        }
                    }
                } else {
                    troubleShootService.stop();
                    troubleshoot.setText("Troubleshoot");
                }
            }
        });
        southPanel.add(troubleshoot);
        return southPanel;
    }

    protected String getCaption() {
        return "<html><body><b>"+HP_ALM_INTEGRATION+"</b><br>Values can be later overridden on project level.</body></html>";
    }

    protected AliConfiguration getConfigurationComponent() {
        return ApplicationManager.getApplication().getComponent(AliConfiguration.class);
    }

    public String getDisplayName() {
        return NAME;
    }

    public Icon getIcon() {
        return null;
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        ensureConfiguration();
        return configurationPanel;
    }

    public boolean isModified() {
        ensureConfiguration();
        return !locationField.getText().equals(aliConfiguration.getLocation()) ||
                !domainField.getText().equals(aliConfiguration.getDomain()) ||
                !projectField.getText().equals(aliConfiguration.getProject()) ||
                !usernameField.getText().equals(aliConfiguration.getUsername()) ||
                !passwdField.getText().equals(aliConfiguration.getPassword()) ||
                (storePasswd.isSelected() && storePasswd.isEnabled()) != aliConfiguration.STORE_PASSWORD;
    }

    public void apply() throws ConfigurationException {
        ensureConfiguration();
        aliConfiguration.ALM_LOCATION = locationField.getValue().trim();
        aliConfiguration.ALM_DOMAIN = domainField.getValue().trim();
        aliConfiguration.ALM_PROJECT = projectField.getValue().trim();
        aliConfiguration.ALM_USERNAME = usernameField.getValue().trim();
        aliConfiguration.ALM_PASSWORD = passwdField.getValue();
        aliConfiguration.STORE_PASSWORD = storePasswd.isEnabled() && storePasswd.isSelected();

        aliConfiguration.fireChanged();
    }

    public void reset() {
        ensureConfiguration();
        locationField.setValue(aliConfiguration.ALM_LOCATION);
        domainField.setValue(aliConfiguration.ALM_DOMAIN);
        projectField.setValue(aliConfiguration.ALM_PROJECT);
        usernameField.setValue(aliConfiguration.ALM_USERNAME);
        passwdField.setValue(aliConfiguration.ALM_PASSWORD);
        storePasswd.setSelected(aliConfiguration.STORE_PASSWORD);
        enableDisableTest();
    }

    protected void enableDisableTest() {
        testLabel.setText("");
        testButton.setEnabled(!domainField.getText().isEmpty() && !projectField.getText().isEmpty() && !locationField.getText().isEmpty() && !usernameField.getText().isEmpty());
        storePasswd.setEnabled(!passwdField.getValue().isEmpty());
    }

    public void disposeUIResources() {
    }

    private void ensureConfiguration() {
        if(aliConfiguration == null) {
            initialize();
            aliConfiguration = getConfigurationComponent();
        }
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

    protected ConfigurationField getLocationField() {
        return new MyTextField(32);
    }

    protected ConfigurationField getUsernameField() {
        return new MyTextField(12);
    }

    protected ConfigurationField getPasswordField() {
        return new MyPasswordField(12);
    }

    protected ConfigurationField getDomainField() {
        return new MyTextField(12);
    }

    protected ConfigurationField getProjectField() {
        return new MyTextField(12);
    }

    public String getId() {
        return "HP";
    }

    public Runnable enableSearch(String option) {
        return null;
    }

    public static ServerType getServerType(String location, String domain, String project, String username, String password) throws AuthenticationFailed {
        RestClient restClient = RestService.createRestClient(location, domain, project, username, password, RestClient.SessionStrategy.NONE);
        return getServerType(restClient, true);
    }

    public static ServerType getServerType(RestClient restClient, boolean loginLogout) throws AuthenticationFailed {
        try {
            if(loginLogout) {
                restClient.login();
            }
            // check for at least ALM 11
            RestService.getForString(restClient, "defects?query={0}", EntityQuery.encode("{id[0]}"));

            try {
                InputStream is = restClient.getForStream("customization/extensions");
                return checkServerType(ProjectExtensionsList.create(is));
            } catch (HttpClientErrorException e){
                if(e.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
                    return checkServerTypeOldStyle(restClient);
                }
                throw e;
            }
        } catch(HttpClientErrorException e) {
            if(e.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED) {
                throw new AuthenticationFailed();
            } else {
                throw new RuntimeException("Failed to connect to HP ALM: " + handleGenericException(restClient, restClient.getDomain(), restClient.getProject()));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to connect to HP ALM: " + handleGenericException(restClient, restClient.getDomain(), restClient.getProject()));
        } finally {
            if(loginLogout) {
                RestService.logout(restClient);
            }
        }
    }

    /*
     * Check server type on the basis of REST service document. This way is valid for ALM 11.5X version and higher
     * @param projectExtensionsList parsed list of extensions and theirs version enabled on the project
     * @return server type
     */
    private static ServerType checkServerType(ProjectExtensionsList projectExtensionsList) {
        String version = null;
        // check for ALM version
        //TODO PD: There should be check for existence of ALI extension (HP ALM 11.5X or higher). ALI extension isn't present by default
        for (String[] ext : projectExtensionsList) {
           if ("QUALITY_CENTER".equals(ext[0])) {
               version = ext[1];
           }
        }

        if(version.startsWith("11.5")) {
            return ServerType.ALM11_5;
        } else if(version.startsWith("12.")) {
            return ServerType.ALM12;
        }
        else {
            return ServerType.AGM;
        }
    }
    /*
     * Check server type on the basis of REST service document. It is the old way which should be used just for ALM 11.00
     * @param restClient rest client
     * @return server type
     */
    private static ServerType checkServerTypeOldStyle(RestClient restClient) {
        // check for ALM version
        String xml;
        try {
            xml = RestService.getForString(restClient, "../../../../?alt=application%2Fatomsvc%2Bxml");
        } catch (RestException e) {
            return ServerType.AGM;
        }

        if(xml.contains("{project}/build-instances")) {
            if(aliEnabledProject(restClient)) {
                return ServerType.ALI2;
            } else {
                return ServerType.ALM11;
            }
        } else if(xml.contains("{project}/changesets")) {
            if(aliEnabledProject(restClient)) {
                return ServerType.ALI;
            } else {
                return ServerType.ALM11;
            }
        } else {
            return ServerType.ALM11;
        }
    }

    private static boolean aliEnabledProject(RestClient restClient) {
        try {
            RestService.getForString(restClient, "changesets?query={0}", EntityQuery.encode("{id[0]}"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String handleGenericException(RestClient restClient, String domain, String project) {
        try {
            if(!containsInsensitive(restClient.listDomains(), domain)) {
                return "domain doesn't exist";
            } else if(!containsInsensitive(restClient.listCurrentProjects(), project)) {
                return "project doesn't exist";
            } else {
                // shouldn't happen
                return "verify connection parameters";
            }
        } catch(Exception ex) {
            return "verify connection parameters";
        }
    }

    private static boolean containsInsensitive(Collection<String> c, String val) {
        TreeSet<String> ts = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        ts.addAll(c);
        return ts.contains(val);
    }
}
