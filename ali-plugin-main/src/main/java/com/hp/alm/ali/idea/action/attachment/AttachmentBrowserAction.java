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
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.progress.task.DownloadTask;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttachmentBrowserAction extends EntityAction {

    public AttachmentBrowserAction() {
        super("Web Browser", "View in web browser", IconLoader.getIcon("/general/web.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("attachment");
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, Entity entity) {
        String filename = entity.getPropertyValue("name");
        if(filename.endsWith(".url")) {
            EntityRef parent = new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id")));
            openAttachmentUrl(project, filename, parent, Integer.valueOf(entity.getPropertyValue("file-size")));
        } else {
            project.getComponent(RestService.class).launchProjectUrl(entity.getPropertyValue("parent-type") + "s/" + entity.getPropertyValue("parent-id") + "/attachments/" + EntityQuery.encode(filename) + "?alt=application/octet-stream");
        }
    }

    public static void openAttachmentUrl(final Project project, String name, EntityRef parent, int size) {
        try {
            final File file = File.createTempFile("tmp", "_" + name);
            file.deleteOnExit();
            ProgressManager.getInstance().run(new DownloadTask(project, file, name, size, parent) {
                @Override
                public void run(ProgressIndicator indicator) {
                    super.run(indicator);
                    if(file.exists()) {
                        try {
                            FileReader reader = new FileReader(file);
                            char[] buf = new char[16384];
                            int n = reader.read(buf);
                            String str = new String(buf, 0, n);
                            Matcher matcher = Pattern.compile("^URL=(.+)$", Pattern.MULTILINE).matcher(str);
                            if(matcher.find()) {
                                BrowserUtil.launchBrowser(matcher.group(1));
                            }
                            reader.close();
                            file.delete();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
