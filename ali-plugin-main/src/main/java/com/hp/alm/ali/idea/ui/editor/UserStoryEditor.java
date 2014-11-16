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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.services.WorkspaceService;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.ui.editor.field.LookupListField;
import com.hp.alm.ali.idea.ui.editor.field.ReferenceField;
import com.hp.alm.ali.idea.ui.editor.field.SpinnerField;
import com.hp.alm.ali.idea.ui.editor.field.TextField;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.services.ApmUIService;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.awt.Dimension;
import java.util.List;

public class UserStoryEditor extends BaseEditor implements BaseEditor.SaveHandler {

    private Project project;
    private ApmUIService apmUIService;
    private WorkspaceService workspaceService;
    private int releaseId;
    private int sprintId;
    private int teamId;

    public UserStoryEditor(Project project, int releaseId, int sprintId, int teamId) {
        super(project, "Create User Story", new Entity("requirement", 0), null);

        this.saveHandler = this;
        this.project = project;
        this.apmUIService = project.getComponent(ApmUIService.class);
        this.workspaceService = project.getComponent(WorkspaceService.class);
        this.releaseId = releaseId;
        this.sprintId = sprintId;
        this.teamId = teamId;

        setSize(new Dimension(640, 480));
        centerOnOwner();
    }

    @Override
    public void update() {
        Metadata metadata = project.getComponent(MetadataService.class).getEntityMetadata("requirement");
        Field priority = metadata.getField("req-priority");
        Field feature = metadata.getField("release-backlog-item.feature-id");
        List<String> list = project.getComponent(ProjectListService.class).getProjectList("requirement", priority);

        addField("name", new TextField(project, "Name", "", true, true));
        addField("description", new HTMLAreaField(project, "Description", "", true, true), true);
        addField("feature", new ReferenceField(project, feature, new Context(entity), true));
        addField("priority", new LookupListField(list, priority, entity, true));
        addField("story-points", new SpinnerField("Story points", "0", false));
    }

    @Override
    public boolean save(Entity modified, Entity base) {
        String feature = getField("feature").getValue();
        int featureId = feature.isEmpty() ? -1 : Integer.valueOf(feature);
        Entity requirement = apmUIService.createRequirementInRelease(
                getField("description").getValue(),
                getField("name").getValue(),
                getField("priority").getValue(),
                Integer.valueOf(getField("story-points").getValue()),
                releaseId,
                sprintId,
                teamId,
                featureId,
                workspaceService.getWorkspaceId());
        if(requirement != null) {
            AliContentFactory.loadDetail(project, requirement, true, true);
            return true;
        } else {
            return false;
        }
    }
}
