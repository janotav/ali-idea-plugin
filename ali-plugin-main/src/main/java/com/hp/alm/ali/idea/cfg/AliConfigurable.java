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
import com.hp.alm.ali.idea.impl.SpellCheckerManager;
import com.hp.alm.ali.idea.model.parser.ProjectExtensionsList;
import com.hp.alm.ali.idea.rest.RestException;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.TroubleShootService;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.rest.client.exception.HttpClientErrorException;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.apache.commons.httpclient.HttpStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;

public class AliConfigurable extends AliAbstractConfigurable implements DocumentListener {

    private AliConfiguration aliConfiguration;
    private JCheckBox spellChecker;
    private JCheckBox devMotiveAnnotation;

    public static String DISPLAY_NAME = "HPE ALI (global)";

    public String getId() {
        return "HPE_ALI_ide";
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    protected void onConfigurationPanelInitialized() {
        SearchableOptionsRegistrar.getInstance().addOption("spelling", null, HP_ALM_INTEGRATION, getId(), getDisplayName());
    }

    protected void addAdditionalSettings(JPanel panel, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy++;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setBorder(new EmptyBorder(10, 0, 0, 0));
        panel.add(spacer, c);

        spellChecker = new JCheckBox("Enable spell checker");
        spellChecker.setSelected(aliConfiguration.spellChecker);
        if (!SpellCheckerManager.isAvailable()) {
            spellChecker.setEnabled(false);
            spellChecker.setToolTipText("feature not available for this IDE version");
        }
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 3;
        panel.add(spellChecker, c);

        devMotiveAnnotation = new JCheckBox("Enable annotations");
        devMotiveAnnotation.setSelected(aliConfiguration.devMotiveAnnotation);
        c.gridy++;
        panel.add(devMotiveAnnotation, c);
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

    public boolean isModified() {
        ensureConfiguration();
        if (super.isModified(
                aliConfiguration.getLocation(),
                aliConfiguration.getDomain(),
                aliConfiguration.getProject(),
                aliConfiguration.getUsername(),
                aliConfiguration.getPassword(),
                aliConfiguration.STORE_PASSWORD)) {
            return true;
        }
        if (devMotiveAnnotation.isSelected() != aliConfiguration.devMotiveAnnotation) {
            return true;
        }
        return spellChecker.isSelected() != aliConfiguration.spellChecker;
    }

    public void apply() throws ConfigurationException {
        ensureConfiguration();
        aliConfiguration.ALM_LOCATION = locationField.getValue().trim();
        aliConfiguration.ALM_DOMAIN = domainField.getValue().trim();
        aliConfiguration.ALM_PROJECT = projectField.getValue().trim();
        aliConfiguration.ALM_USERNAME = usernameField.getValue().trim();
        aliConfiguration.ALM_PASSWORD = passwdField.getValue();
        aliConfiguration.STORE_PASSWORD = storePasswd.isEnabled() && storePasswd.isSelected();
        aliConfiguration.spellChecker = spellChecker.isSelected();
        aliConfiguration.devMotiveAnnotation = devMotiveAnnotation.isSelected();
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
        spellChecker.setSelected(aliConfiguration.spellChecker);
        devMotiveAnnotation.setSelected(aliConfiguration.devMotiveAnnotation);
        enableDisableTest();
    }

    protected void loadConfiguration() {
        aliConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
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
                throw new RuntimeException("Failed to connect to HPE ALM: " + handleGenericException(restClient, restClient.getDomain(), restClient.getProject()));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to connect to HPE ALM: " + handleGenericException(restClient, restClient.getDomain(), restClient.getProject()));
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
        String qcVersion = null;
        String aliVersion = null;

        for (String[] ext : projectExtensionsList) {
           if ("QUALITY_CENTER".equals(ext[0])) {
               qcVersion = ext[1];
           }

           if ("ALI_EXTENSION".equals(ext[0])) {
               aliVersion = ext[1];
           }

           if ("APM_EXTENSION".equals(ext[0])) {
               return ServerType.AGM;
           }
        }

        if(qcVersion != null && qcVersion.startsWith("11.5")) {
            if(aliVersion != null) {
                return ServerType.ALI11_5;
            } else {
                return ServerType.ALM11_5;
            }
        } else {
            // assume latest version compatibility
            if(aliVersion != null) {
                return ServerType.ALI12;
            } else {
                return ServerType.ALM12;
            }
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
