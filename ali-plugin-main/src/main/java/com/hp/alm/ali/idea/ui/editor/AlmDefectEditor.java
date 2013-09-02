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

package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.CustomizationService;
import com.hp.alm.ali.idea.ui.editor.field.EditableField;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.List;

public class AlmDefectEditor extends EntityEditor {

    public AlmDefectEditor(Project project, String titleTemplate, final Entity entity, List<String> columnsToEdit, boolean addRequired, boolean removeReadonly, List<String> forceEditable, SaveHandler saveHandler) {
        super(project, titleTemplate, entity, filterColumns(columnsToEdit), addRequired, removeReadonly, forceEditable, saveHandler);
    }

    @Override
    public void update() {
        super.update();
        CustomizationService customizationService = project.getComponent(CustomizationService.class);
        final EditableField statusField = getField("status");
        String newStatus = customizationService.getNewDefectStatus(new AbstractCachingService.DispatchCallback<String>() {
            @Override
            public void loaded(String value) {
                if(!value.isEmpty()) {
                    statusField.setValue(value);
                }
                statusField.getComponent().setEnabled(true);
            }
        });
        if(newStatus == null) {
            statusField.getComponent().setEnabled(false);
        } else if(!newStatus.isEmpty()) {
            statusField.setValue(newStatus);
        }

    }

    private static List<String> filterColumns(List<String> columnsToEdit) {
        columnsToEdit.remove("comments");
        columnsToEdit.remove("dev-comments");
        if(!columnsToEdit.contains("status")) {
            columnsToEdit.add("status");
        }
        return columnsToEdit;
    }
}
