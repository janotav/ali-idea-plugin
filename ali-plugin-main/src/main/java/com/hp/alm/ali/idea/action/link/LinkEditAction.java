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

package com.hp.alm.ali.idea.action.link;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LinkEditAction extends EntityAction {

    public LinkEditAction() {
        super("Modify", "Modify link", IconLoader.getIcon("/actions/editSource.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("defect-link");
    }

    @Override
    protected void update(AnActionEvent event, Project project, Entity entity) {
        EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        event.getPresentation().setEnabled(!entityEditManager.isEditing(entity));
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        List<String> columns = new ArrayList<String>(project.getComponent(RestService.class).getServerStrategy().getDefectLinkColumns());
        final EntityService entityService = project.getComponent(EntityService.class);
        final Entity defectLink = entityService.getDefectLink(Integer.valueOf(entity.getPropertyValue("first-endpoint-id")), entity.getId());

        EntityEditor editor = new EntityEditor(project, "Modify {0}", defectLink, columns, true, false, Collections.<String>emptyList(), new BaseEditor.SaveHandler() {
            @Override
            public boolean save(Entity modifiedLink, Entity base) {
                // first-endpoint-id is needed during ALM 11 compatible update
                modifiedLink.setProperty("first-endpoint-id", defectLink.getPropertyValue("first-endpoint-id"));
                return entityService.updateEntity(modifiedLink, null, false) != null;
            }
        });
        editor.execute();
    }
}
