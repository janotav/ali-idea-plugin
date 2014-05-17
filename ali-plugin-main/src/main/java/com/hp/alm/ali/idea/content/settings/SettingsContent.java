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

package com.hp.alm.ali.idea.content.settings;

import com.hp.alm.ali.idea.content.AliContent;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class SettingsContent implements AliContent {

    private static SettingsContent instance = new SettingsContent();

    public static SettingsContent getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Settings";
    }

    @Override
    public JComponent create(Project project) {
        return new SettingsPanel(project, UIManager.getDefaults().getColor("EditorPane.background"));
    }
}
