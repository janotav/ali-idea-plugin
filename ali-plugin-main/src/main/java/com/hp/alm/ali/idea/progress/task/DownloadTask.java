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
import com.hp.alm.ali.idea.rest.MyResultInfo;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.progress.CanceledException;
import com.hp.alm.ali.idea.progress.IndicatingOutputStream;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;

public class DownloadTask extends Task.Backgroundable {

    private File file;
    private String sourceFilename;
    private int size;
    private EntityRef entity;

    private RestService restService;

    public DownloadTask(Project project, File file, String sourceFilename, int size, EntityRef entity) {
        super(project, "Download attachment: "+file.getName());

        this.file = file;
        this.sourceFilename = sourceFilename;
        this.size = size;
        this.entity = entity;

        this.restService = project.getComponent(RestService.class);
    }

    public void run(ProgressIndicator indicator) {
        try {
            IndicatingOutputStream ios = new IndicatingOutputStream(file, size, indicator);
            restService.get(new MyResultInfo(ios), "{0}s/{1}/attachments/{2}?alt=application/octet-stream", entity.type, entity.id, EntityQuery.encode(sourceFilename));
            ios.close();
        } catch(CanceledException e) {
            // when canceled purposefully remove the incomplete file
            file.delete();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
