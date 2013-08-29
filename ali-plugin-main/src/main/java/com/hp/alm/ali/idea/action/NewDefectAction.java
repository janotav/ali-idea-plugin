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

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.ui.editor.AlmDefectEditor;
import com.hp.alm.ali.idea.ui.editor.DefectEditor;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NewDefectAction extends ConnectedAction {

    public NewDefectAction() {
        super("New defect", "Create new defect", IconLoader.getIcon("/new_defect_16.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project != null) {
            RestService restService = project.getComponent(RestService.class);

            ServerType serverType = restService.getServerTypeIfAvailable();
            if(serverType == ServerType.AGM) {
                createDefectAgm(project);
            } else {
                createDefectAlm(project);
            }
        }
    }

    private void createDefectAgm(Project project) {
        new DefectEditor(project, -1, -1, -1).execute();
    }

    private void createDefectAlm(final Project project) {
        AliProjectConfiguration projConf = project.getComponent(AliProjectConfiguration.class);
        EntityEditManager editManager = project.getComponent(EntityEditManager.class);

        List<String> fields = editManager.getEditorFields("defect");
        Entity newEntity = new Entity("defect", 0);

        newEntity.setProperty("detected-by", projConf.getUsername());
        newEntity.setProperty("creation-time", CommentField.dateFormat.format(new Date()));

        AlmDefectEditor entityEditor = new AlmDefectEditor(project, "Create New {0}", newEntity, fields, true, true, Collections.<String>emptyList(), new EntityEditor.Create(project) {
            @Override
            protected void entityCreated(Entity entity) {
                AliContentFactory.loadDetail(project, entity, true, true);
            }
        });
        entityEditor.execute();
    }
}
