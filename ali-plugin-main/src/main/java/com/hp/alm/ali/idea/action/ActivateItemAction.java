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

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

import java.util.HashSet;
import java.util.Set;

public class ActivateItemAction extends ToggleAction {

    private static Set<String> workItemTypes;
    static {
        workItemTypes = new HashSet<String>();
        workItemTypes.add("requirement");
        workItemTypes.add("defect");
        workItemTypes.add("release-backlog-item"); // make sure we are visible in the EntityQuery("release-backlog-item") context
    }

    public ActivateItemAction() {
        super("Activate", "Activate Work Item", ActiveItemService.activeItemIcon);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e); // handles selection state
        e.getPresentation().setEnabled(getWorkItem(e) != null && ConnectedAction.isConnected(e));
        boolean visible = workItemTypes.contains(EntityAction.getEntityType(e));
        e.getPresentation().setVisible(visible);
        if(visible) {
            boolean selected = isSelected(e);
            e.getPresentation().setText(selected? "Deactivate": "Activate");
            e.getPresentation().setDescription(selected? "Deactivate work item": "Activate work item");
        }
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project != null) {
            EntityRef activeItem = project.getComponent(ActiveItemService.class).getActiveItem();
            Entity entity = getWorkItem(event);
            return isActiveEntity(entity, activeItem);
        } else {
            return false;
        }
    }

    @Override
    public void setSelected(AnActionEvent event, boolean selected) {
        Project project = getEventProject(event);
        if(project != null) {
            ActiveItemService activeItemService = project.getComponent(ActiveItemService.class);
            EntityRef activeItem = activeItemService.getActiveItem();
            Entity entity = EntityAction.getEntity(event);
            boolean isActive = isActiveEntity(entity, activeItem);
            if(!selected && isActive) {
                activeItemService.activate(null, true, false);
            } else if(selected && !isActive) {
                activeItemService.activate(entity, true, false);
            }
        }
    }

    private Entity getWorkItem(AnActionEvent event) {
        Entity entity = EntityAction.getEntity(event);
        if(entity != null && workItemTypes.contains(entity.getType())) {
            return entity;
        } else {
            return null;
        }
    }

    private boolean isActiveEntity(Entity entity, EntityRef activeItem) {
        return entity != null && activeItem != null && entity.getId()== activeItem.id && entity.getType().equals(activeItem.type);
    }
}
