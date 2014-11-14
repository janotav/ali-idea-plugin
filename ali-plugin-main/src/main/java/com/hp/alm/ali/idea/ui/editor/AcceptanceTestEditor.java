/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategyImpl;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.ui.editor.field.LookupListField;
import com.hp.alm.ali.idea.ui.editor.field.TextAreaField;
import com.intellij.openapi.project.Project;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AcceptanceTestEditor extends BaseEditor {

    private Metadata metadata;
    private Field statusField;
    private List<String> statusValues;

    public AcceptanceTestEditor(Project project, EntityRef parent) {
        super(project, "Add Acceptance Test", new Entity("acceptance-test", 0), new AcceptanceTestEditor.Create(project, parent));

        entity.setProperty("status", "Not Started");

        init();
    }

    public AcceptanceTestEditor(Project project, Entity test) {
        super(project, "Modify Acceptance Test", test, new EntityEditStrategyImpl.Edit(project));

        init();
    }

    private void init() {
        setSize(new Dimension(640, 480));
        centerOnOwner();
    }

    public void beforeUpdate() {
        metadata = project.getComponent(MetadataService.class).getEntityMetadata("acceptance-test");
        statusField = metadata.getField("status");
        statusValues = project.getComponent(ProjectListService.class).getProjectList("acceptance-test", statusField);
    }

    @Override
    public void update() {
        addField("description", new TextAreaField("Description", entity.getPropertyValue("description"), true, true), true);
        addField("status", new LookupListField(statusValues, statusField, entity, true, true));
    }

    public static class Create implements SaveHandler {

        private EntityService entityService;
        private EntityRef parent;

        public Create(Project project, EntityRef parent) {
            this.parent = parent;
            entityService = project.getComponent(EntityService.class);
        }

        @Override
        public boolean save(Entity modified, Entity entity) {
            modified.setProperty("entity-id", String.valueOf(parent.id));
            modified.setProperty("entity-type", parent.type);
            Date now = new Date();
            modified.setProperty("last-modified-status-date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now));
            modified.setProperty("creation-date", new SimpleDateFormat("yyyy-MM-dd").format(now));
            return entityService.createEntity(modified, false) != null;
        }
    }
}
