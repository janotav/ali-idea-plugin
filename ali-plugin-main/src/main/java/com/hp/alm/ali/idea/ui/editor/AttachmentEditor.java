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

import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.ui.editor.field.UploadField;
import com.hp.alm.ali.idea.ui.editor.field.TextField;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

public class AttachmentEditor extends BaseEditor {

    private static String invalidChars = "/\\:*?\"'<>|";

    public static final String FIELD_UPLOAD = "upload";
    public static final String FIELD_FILENAME = "name";
    public static final String FIELD_DESCRIPTION= "description";
    private final boolean needsContent;
    private final boolean editableFilename;

    public AttachmentEditor(Project project, String title, Entity entity, boolean needsContent, boolean editableFilename, SaveHandler saveHandler) {
        super(project, title, entity, saveHandler);
        this.needsContent = needsContent;
        this.editableFilename = editableFilename;
    }

    public String validate(StringBuffer message) {
        for(int i = 0; i < invalidChars.length(); i++) {
            if(getField(FIELD_FILENAME).getValue().indexOf(invalidChars.charAt(i)) >= 0) {
                message.append("File name must not contain any of the following characters:\n\n").append(invalidChars);
                return "Invalid file name";
            }
        }
        return null;
    }

    @Override
    public void update() {
        addField(FIELD_UPLOAD, new UploadField("Upload Content", needsContent, true));
        addField(FIELD_FILENAME, new TextField("File Name", entity.getPropertyValue(FIELD_FILENAME), false, editableFilename));
        addField(FIELD_DESCRIPTION, new HTMLAreaField("Description", entity.getPropertyValue(FIELD_DESCRIPTION), false, true), true);

        packAndPosition();
    }
}
