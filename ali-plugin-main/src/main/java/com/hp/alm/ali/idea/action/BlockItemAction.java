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

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.EntityService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BlockItemAction extends ToggleAction {

    private static Set<String> workItemTypes;
    static {
        workItemTypes = new HashSet<String>();
        workItemTypes.add("requirement");
        workItemTypes.add("defect");
    }

    public BlockItemAction() {
        super("Block", "Mark item as blocked", IconLoader.getIcon("/blocked.png"));
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e); // handles selection state
        Entity entity = getWorkItem(e);
        boolean hasBacklogItemId = entity != null && !entity.getPropertyValue("release-backlog-item.id").isEmpty();
        boolean enabled = hasBacklogItemId && ConnectedAction.isConnected(e);
        e.getPresentation().setEnabled(enabled);
        Project project = getEventProject(e);
        boolean hasBacklogItem = project != null && ServerType.AGM.equals(project.getComponent(RestService.class).getServerTypeIfAvailable());
        boolean visible = hasBacklogItem && workItemTypes.contains(EntityAction.getEntityType(e));
        e.getPresentation().setVisible(visible);
        if(visible) {
            String reason = getBlockedReason(entity);
            if (!StringUtils.isEmpty(reason)) {
                e.getPresentation().setText("Unblock: " + reason);
                e.getPresentation().setDescription("Unblock work item: " + reason);
            } else {
                e.getPresentation().setText("Block");
                e.getPresentation().setDescription("Block work item");
            }
        }
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project != null) {
            Entity entity = getWorkItem(event);
            return isBlockedEntity(entity);
        } else {
            return false;
        }
    }

    @Override
    public void setSelected(AnActionEvent event, boolean selected) {
        Project project = getEventProject(event);
        if(project != null) {
            Entity entity = EntityAction.getEntity(event);
            boolean isBlocked = isBlockedEntity(entity);
            Entity backlogItem = new Entity("release-backlog-item", Integer.valueOf(entity.getPropertyValue("release-backlog-item.id")));
            if(!selected && isBlocked) {
                backlogItem.setProperty("blocked", null);
            } else if(selected && !isBlocked) {
                String reason = Messages.showInputDialog(project, "Provide the reason why the item is blocked", "Blocking Reason", null);
                if (reason != null) {
                    backlogItem.setProperty("blocked", reason);
                } else {
                    return;
                }
            } else {
                return;
            }
            EntityService entityService = project.getComponent(EntityService.class);
            entityService.updateEntity(backlogItem, Collections.singleton("blocked"), false);
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

    private boolean isBlockedEntity(Entity entity) {
        return !StringUtils.isEmpty(getBlockedReason(entity));
    }

    private String getBlockedReason(Entity entity) {
        if (entity != null) {
            return (String) entity.getProperty("release-backlog-item.blocked");
        } else {
            return null;
        }
    }
}
