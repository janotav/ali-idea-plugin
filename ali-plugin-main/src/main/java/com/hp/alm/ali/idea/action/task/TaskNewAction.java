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

package com.hp.alm.ali.idea.action.task;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.ui.editor.TaskEditor;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TaskNewAction extends EntityAction {

    private static Set<String> entityTypes;
    static {
        entityTypes = new HashSet<String>();
        entityTypes.add("defect");
        entityTypes.add("requirement");
    }

    public TaskNewAction() {
        super("New", "Create task", IconLoader.getIcon("/general/add.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return entityTypes;
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, Entity entity) {
        project.getComponent(EntityService.class).requestCachedEntity(new EntityRef(entity), Collections.<String>singletonList("release-backlog-item.id"), new EntityAdapter() {
            @Override
            public void entityLoaded(Entity entity, Event event) {
                TaskEditor editor = new TaskEditor(project, Integer.valueOf(entity.getPropertyValue("release-backlog-item.id")));
                editor.execute();
            }
        });
    }
}
