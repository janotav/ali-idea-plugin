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

package com.hp.alm.ali.idea.filter;

import com.hp.alm.ali.idea.ui.chooser.PopupDialog;
import com.intellij.openapi.project.Project;

import java.util.List;

public class FilterFactoryImpl implements FilterFactory {

    private final Project project;
    private boolean multiple;
    private String targetType;
    private boolean compound;

    public FilterFactoryImpl(Project project, String targetType, boolean multiple, boolean compound) {
        this.project = project;
        this.multiple = multiple;
        this.targetType = targetType;
        this.compound = compound;
    }

    @Override
    public FilterChooser createChooser(String value) {
        PopupDialog popup;
        if(compound) {
            // plain integer reference (e.g. release-backlog-item.sprint-id)
            popup = new PopupDialog(project, targetType, false, multiple, multiple? PopupDialog.Selection.APPENDING_ID: PopupDialog.Selection.FOLLOW_ID, true);
        } else {
            // old-school ^Releases\Release^ references
            popup = new PopupDialog(project, targetType, false, multiple, multiple? PopupDialog.Selection.APPENDING: PopupDialog.Selection.FOLLOW, true);
        }
        popup.setValue(value);
        return popup;
    }

    @Override
    public List<String> getCustomChoices() {
        return null;
    }
}
