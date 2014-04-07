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

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.editor.field.SpinnerField;
import com.hp.alm.ali.idea.ui.editor.field.TextAreaField;
import com.hp.alm.ali.idea.ui.editor.field.UserField;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.awt.Dimension;


public class TaskEditor extends BaseEditor {

    protected EntityService entityService;

    public TaskEditor(Project project, int itemId) {
        super(project, "Add task", new Entity("project-task", 0), new TaskEditor.Create(project));

        entityService = project.getComponent(EntityService.class);

        entity.setProperty("release-backlog-item-id", String.valueOf(itemId));

        setSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
    }

    public TaskEditor(Project project, Entity task) {
        this(project, task, new Edit(project));
    }

    public TaskEditor(Project project, Entity task, SaveHandler saveHandler) {
        super(project, "Edit task", task, saveHandler);

        entityService = project.getComponent(EntityService.class);

        setSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
    }

    @Override
    public void update() {
        addField("description", new TextAreaField("Description", entity.getPropertyValue("description"), true, true), true);
        addField("assigned-to", new UserField("Team Member", entity.getPropertyValue("assigned-to"), project, false));
        if(entity.getId() > 0) {
            addField("invested", new SpinnerField("Invested", entity.getPropertyValue("invested"), true));
            addField("remaining", new SpinnerField("Remaining", entity.getPropertyValue("remaining"), true));
            addField("estimated", new SpinnerField("Estimated", entity.getPropertyValue("estimated"), true));
        } else {
            addField("estimated", new SpinnerField("Estimated", "6", true));
        }
    }

    public static class Edit implements SaveHandler {

        private EntityService entityService;

        public Edit(Project project) {
            entityService = project.getComponent(EntityService.class);
        }

        @Override
        public boolean save(Entity modified, Entity entity) {
            modified.setProperty("release-backlog-item-id", entity.getPropertyValue("release-backlog-item-id"));

            final Entity updatedTask = entityService.updateEntity(modified, null, false);
            if(updatedTask != null) {
                if(modified.isInitialized("invested") || modified.isInitialized("remaining")) {
                    entityService.getEntityAsync(new EntityRef("release-backlog-item", Integer.valueOf(entity.getPropertyValue("release-backlog-item-id"))), null);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static class Create implements SaveHandler {

        private EntityService entityService;

        public Create(Project project) {
            entityService = project.getComponent(EntityService.class);
        }

        @Override
        public boolean save(Entity modified, Entity entity) {
            modified.setProperty("release-backlog-item-id", entity.getPropertyValue("release-backlog-item-id"));
            modified.setProperty("invested", "0");
            modified.setProperty("remaining", modified.getProperty("estimated"));

            int itemId = Integer.valueOf(entity.getPropertyValue("release-backlog-item-id"));
            Entity createdTask = entityService.createEntity(modified, false);
            if (createdTask != null) {
                entityService.getEntityAsync(new EntityRef("release-backlog-item", itemId), null);
                return true;
            } else {
                return false;
            }
        }
    }
}
