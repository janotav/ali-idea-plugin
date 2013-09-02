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
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ui.UIUtil;

import javax.swing.UIManager;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class AttachmentOpenAction extends EntityAction {

    public AttachmentOpenAction() {
        super("Open", "Download and open locally", UIManager.getIcon("Tree.openIcon"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("attachment");
    }

    @Override
    public void update(AnActionEvent event) {
        Entity entity = getEntity(event);
        if(entity != null && "attachment".equals(entity.getType())) {
            String filename = entity.getPropertyValue("name");
            String size = entity.getPropertyValue("file-size");
            if(filename != null && size != null && isAllowed(filename, Integer.valueOf(size))) {
                event.getPresentation().setVisible(true);
                event.getPresentation().setEnabled(true);
                return;
            }
        }
        event.getPresentation().setVisible(false);
        event.getPresentation().setEnabled(false);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        openAttachment(project, entity.getPropertyValue("name"), new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id"))), Integer.valueOf(entity.getPropertyValue("file-size")));
    }

    public static boolean isAllowed(String filename, int size) {
        if(filename.endsWith(".agmlink") || filename.endsWith(".url") || size > 10000000) {
            // open neither Maya nor AgM style hyperlinks
            // avoid big files too
            return false;
        } else {
            return true;
        }
    }

    public static void openAttachment(final Project project, String name, EntityRef parent, int size) {
        try {
            final File file = File.createTempFile("tmp", "_" + name);
            ProgressManager.getInstance().run(new AttachmentDownloadTask(project, file, name, size, parent) {
                @Override
                public void run(ProgressIndicator indicator) {
                    super.run(indicator);
                    if(file.exists()) {
                        String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, FileUtil.toSystemIndependentName(file.getAbsolutePath()));
                        final VirtualFile virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            public void run() {
                                if(virtualFile != null) {
                                    FileEditor[] editors = project.getComponent(FileEditorManager.class).openFile(virtualFile, true);
                                    if(editors.length > 0) {
                                        return;
                                    }
                                }
                                Messages.showWarningDialog("No editor seems to be associated with this file type. Try to download and open the file manually.", "Not Supported");
                            }
                        });
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
