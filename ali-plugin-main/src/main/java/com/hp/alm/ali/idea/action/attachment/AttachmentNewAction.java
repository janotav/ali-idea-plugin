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

package com.hp.alm.ali.idea.action.attachment;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.ui.editor.AttachmentEditor;
import com.hp.alm.ali.idea.progress.task.UploadTask;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class AttachmentNewAction extends EntityAction {

    private static Set<String> entityTypes;
    static {
        entityTypes = new HashSet<String>();
        entityTypes.add("defect");
        entityTypes.add("requirement");
    }

    public AttachmentNewAction() {
        super("Upload", "Upload attachment", IconLoader.getIcon("/general/add.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return entityTypes;
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, final Entity entity) {
        AttachmentEditor editor = new AttachmentEditor(project, "Add Attachment", new Entity("attachment", 0), true, true, new BaseEditor.SaveHandler() {
            @Override
            public boolean save(Entity modified, Entity base) {
                addAttachment(project, entity, modified);
                return true;
            }
        });
        editor.execute();
    }

    private void addAttachment(Project project, Entity parent, final Entity att) {
        final File file = new File(att.getPropertyValue(AttachmentEditor.FIELD_UPLOAD));
        String filename = att.getPropertyValue(AttachmentEditor.FIELD_FILENAME);
        if(filename.isEmpty()) {
            filename = file.getName();
        }
        ProgressManager.getInstance().run(new UploadTask(project, file, filename, att.getPropertyValue(AttachmentEditor.FIELD_DESCRIPTION), new EntityRef(parent)));
    }
}
