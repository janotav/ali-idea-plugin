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
import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.progress.task.UpdateAttachmentTask;
import com.hp.alm.ali.idea.ui.editor.AttachmentEditor;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.AttachmentService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class AttachmentEditAction extends EntityAction {

    public AttachmentEditAction() {
        super("Modify", "Modify entity", IconLoader.getIcon("/actions/editSource.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("attachment");
    }

    @Override
    protected void update(AnActionEvent event, Project project, Entity entity) {
        EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        event.getPresentation().setEnabled(!entityEditManager.isEditing(entity));
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, Entity entity) {
        final EntityRef parent = new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id")));
        final Entity attachmentEntity = project.getComponent(AttachmentService.class).getAttachmentEntity(entity.getPropertyValue("name"), parent);

        String name = entity.getPropertyValue("name");
        AttachmentEditor editor = new AttachmentEditor(project, "Modify Attachment: " + name, attachmentEntity, false, project.getComponent(RestService.class).getServerTypeIfAvailable() == ServerType.ALM12, new BaseEditor.SaveHandler() {
            @Override
            public boolean save(Entity modified, Entity base) {
                updateAttachment(project, parent, attachmentEntity, modified);
                return true;
            }
        });
        editor.execute();
    }

    private void updateAttachment(Project project, EntityRef parent, Entity attEntity, Entity modified) {
        String name = attEntity.getPropertyValue("name");
        String filePath = modified.getPropertyValue(AttachmentEditor.FIELD_UPLOAD);
        File file;
        if(!filePath.isEmpty()) {
            file = new File(filePath);
        } else {
            file = null;
        }
        ProgressManager.getInstance().run(new UpdateAttachmentTask(project, file, name, (String)modified.getProperty("name"), (String)modified.getProperty("description"), parent));
    }
}
