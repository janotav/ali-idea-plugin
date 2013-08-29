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
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AliProjectConfigurable extends AliConfigurable {

    private Project project;
    private AliProjectConfiguration projectConfiguration;
    private AliConfiguration ideConfiguration;

    public AliProjectConfigurable(Project project) {
        this.project = project;
    }

    protected Component getSouthernComponent() {
        return null;
    }

    protected String getCaption() {
        return "<html><body><b>"+HP_ALM_INTEGRATION+"</b><br>Project specific values.</body></html>";
    }

    protected AliConfiguration getConfigurationComponent() {
        return projectConfiguration;
    }

    protected ConfigurationField getLocationField() {
        ensureConfiguration();
        return new MergedTextField(32, ideConfiguration.ALM_LOCATION);
    }

    protected ConfigurationField getUsernameField() {
        ensureConfiguration();
        return new MergedTextField(12, ideConfiguration.ALM_USERNAME);
    }

    protected ConfigurationField getDomainField() {
        ensureConfiguration();
        return new MergedTextField(12, ideConfiguration.ALM_DOMAIN);
    }

    protected ConfigurationField getProjectField() {
        ensureConfiguration();
        return new MergedTextField(12, ideConfiguration.ALM_PROJECT);
    }

    protected ConfigurationField getPasswordField() {
        ensureConfiguration();
        return new MergedPasswordField(12, ideConfiguration.ALM_PASSWORD);
    }

    public JComponent createComponent() {
        JComponent component = super.createComponent();
        component.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
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
        return component;
    }

    private void ensureConfiguration() {
        if(projectConfiguration == null) {
            projectConfiguration = project.getComponent(AliProjectConfiguration.class);
            ideConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
        }
    }
}
