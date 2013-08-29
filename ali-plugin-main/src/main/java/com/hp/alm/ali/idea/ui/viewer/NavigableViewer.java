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

package com.hp.alm.ali.idea.ui.viewer;

import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.navigation.NavigationListener;
import com.intellij.openapi.project.Project;

import javax.swing.JTextPane;

public class NavigableViewer extends TextViewer {

    private Project project;

    public NavigableViewer(Project project, String value) {
        super(new JTextPane());
        this.project = project;

        ((JTextPane)getComponent()).addHyperlinkListener(new NavigationListener(project));

        setValue(value);
    }

    @Override
    public void setValue(String value) {
        HTMLAreaField.enableCapability((JTextPane) getComponent(), project, value, false, true);
    }
}
