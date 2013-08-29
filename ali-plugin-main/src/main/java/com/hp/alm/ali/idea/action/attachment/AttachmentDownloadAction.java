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
import com.hp.alm.ali.idea.progress.task.DownloadTask;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;

import java.util.Collections;
import java.util.Set;

public class AttachmentDownloadAction extends EntityAction {

    // static field: share the directory across all entities (and entity refresh)
    private static VirtualFile lastDir;

    public AttachmentDownloadAction() {
        super("Download", "Download", IconLoader.getIcon("/actions/checkOut.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("attachment");
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        String name = entity.getPropertyValue("name");
        FileSaverDescriptor desc = new FileSaverDescriptor("Download Attachment", "Download attachment to the local filesystem.");
        final VirtualFileWrapper file = FileChooserFactory.getInstance().createSaveFileDialog(desc, project).save(lastDir, name);
        if(file != null) {
            VirtualFile vf = file.getVirtualFile(true);
            if(vf == null) {
                Messages.showErrorDialog("Invalid file specified", "Error");
                return;
            }
            lastDir = vf.getParent();
            ProgressManager.getInstance().run(new DownloadTask(project, file.getFile(), name, Integer.valueOf(entity.getPropertyValue("file-size")), new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id")))));
        }
    }
}
