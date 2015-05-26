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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AliProjectConfigurable extends AliAbstractConfigurable {

    private Project project;
    private AliProjectConfiguration projectConfiguration;
    private AliConfiguration ideConfiguration;

    public static String DISPLAY_NAME = "HP ALI (project)";

    public AliProjectConfigurable(Project project) {
        this.project = project;
    }

    public String getId() {
        return "HP_ALI";
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    protected String getCaption() {
        return "<html><body><b>"+HP_ALM_INTEGRATION+"</b><br>Project specific values.</body></html>";
    }

    protected ConfigurationField getLocationField() {
        return new MergedTextField(32, ideConfiguration.ALM_LOCATION);
    }

    protected ConfigurationField getUsernameField() {
        return new MergedTextField(12, ideConfiguration.ALM_USERNAME);
    }

    protected ConfigurationField getDomainField() {
        return new MergedTextField(12, ideConfiguration.ALM_DOMAIN);
    }

    protected ConfigurationField getProjectField() {
        return new MergedTextField(12, ideConfiguration.ALM_PROJECT);
    }

    protected ConfigurationField getPasswordField() {
        return new MergedPasswordField(12, ideConfiguration.ALM_PASSWORD);
    }

    public boolean isModified() {
        ensureConfiguration();
        return super.isModified(
                projectConfiguration.getLocation(),
                projectConfiguration.getDomain(),
                projectConfiguration.getProject(),
                projectConfiguration.getUsername(),
                projectConfiguration.getPassword(),
                projectConfiguration.STORE_PASSWORD);
    }

    public void apply() throws ConfigurationException {
        ensureConfiguration();
        projectConfiguration.ALM_LOCATION = locationField.getValue().trim();
        projectConfiguration.ALM_DOMAIN = domainField.getValue().trim();
        projectConfiguration.ALM_PROJECT = projectField.getValue().trim();
        projectConfiguration.ALM_USERNAME = usernameField.getValue().trim();
        projectConfiguration.ALM_PASSWORD = passwdField.getValue();
        projectConfiguration.STORE_PASSWORD = storePasswd.isEnabled() && storePasswd.isSelected();
        projectConfiguration.fireChanged();
    }

    public void reset() {
        ensureConfiguration();
        locationField.setValue(projectConfiguration.ALM_LOCATION);
        domainField.setValue(projectConfiguration.ALM_DOMAIN);
        projectField.setValue(projectConfiguration.ALM_PROJECT);
        usernameField.setValue(projectConfiguration.ALM_USERNAME);
        passwdField.setValue(projectConfiguration.ALM_PASSWORD);
        storePasswd.setSelected(projectConfiguration.STORE_PASSWORD);
        enableDisableTest();
    }

    protected void onConfigurationPanelInitialized() {
        configurationPanel.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                if(pce.getNewValue() != null) {
                    // defaults might have changed
                    ((MergingField)usernameField).setDefaultValue(ideConfiguration.ALM_USERNAME);
                    ((MergingField)passwdField).setDefaultValue(ideConfiguration.ALM_PASSWORD);
                    ((MergingField)locationField).setDefaultValue(ideConfiguration.ALM_LOCATION);
                    ((MergingField)domainField).setDefaultValue(ideConfiguration.ALM_DOMAIN);
                    ((MergingField)projectField).setDefaultValue(ideConfiguration.ALM_PROJECT);
                }
            }
        });
    }

    protected void loadConfiguration() {
        projectConfiguration = project.getComponent(AliProjectConfiguration.class);
        ideConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
    }
}
