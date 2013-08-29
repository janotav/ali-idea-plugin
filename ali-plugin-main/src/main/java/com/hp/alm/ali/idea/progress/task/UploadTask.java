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

package com.hp.alm.ali.idea.progress.task;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.AttachmentService;
import com.hp.alm.ali.idea.progress.CanceledException;
import com.hp.alm.ali.idea.progress.IndicatingInputStream;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;

import java.io.File;
import java.io.FileNotFoundException;

public class UploadTask extends Task.Backgroundable {

    private File file;
    private String filename;
    private String description;
    private EntityRef parent;
    private AttachmentService attachmentService;

    public UploadTask(Project project, File file, String filename, String description, EntityRef parent) {
        super(project, "Upload attachment: "+filename, true, PerformInBackgroundOption.DEAF);

        this.file = file;
        this.filename = filename;
        this.description = description;
        this.parent = parent;

        this.attachmentService = project.getComponent(AttachmentService.class);
    }

    public void run(ProgressIndicator indicator) {
        IndicatingInputStream fis;
        try {
            fis = new IndicatingInputStream(file, indicator);
        } catch (FileNotFoundException e) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    Messages.showErrorDialog("File not found.", "Failed to upload attachment");
                }
            });
            return;
        }
        try {
            String attachmentName = attachmentService.createAttachment(filename, fis, file.length(), parent);
            if(attachmentName == null) {
                return;
            }

            if(description != null && !indicator.isCanceled()) {
                attachmentService.updateAttachmentProperty(attachmentName, parent, "description", description, false);
            }
        } catch(CanceledException e) {
            // user gave up
        }
    }
}
