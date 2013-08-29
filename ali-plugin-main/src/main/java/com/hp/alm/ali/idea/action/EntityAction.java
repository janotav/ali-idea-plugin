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

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class EntityAction extends ConnectedAction {

    public EntityAction(String name, String description, Icon icon) {
        super(name, description, icon);
    }

    @Override
    public void update(AnActionEvent event) {
        if(!isSupportedPlace(event.getPlace())) {
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
            return;
        }
        if(!isConnected(event)) {
            event.getPresentation().setEnabled(false);
            return;
        }
        Entity entity = getEntity(event);
        String entityType = getEntityType(event);
        Project project = getEventProject(event);
        boolean visible = isSupportedType(entityType) && visiblePredicate(project, entityType);
        boolean enabled = isConnected(event) && entity != null && enabledPredicate(project, entity);
        event.getPresentation().setVisible(visible);
        event.getPresentation().setEnabled(enabled);
        if(enabled) {
            update(event, project, entity);
        }
    }

    protected boolean visiblePredicate(Project project, String entityType) {
        return true;
    }

    protected boolean enabledPredicate(Project project, Entity entity) {
        return true;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Entity entity = getEntity(event);
        if(entity == null) {
            return;
        }

        if(!isSupportedType(entity.getType())) {
            return;
        }

        if(!isConnected(event)) {
            return;
        }

        actionPerformed(event, getEventProject(event), entity);
    }

    public static Entity getEntity(AnActionEvent event) {
        List<Entity> list = (List<Entity>) event.getDataContext().getData("entity-list");
        if(list != null && list.size() == 1) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public static String getEntityType(AnActionEvent event) {
        Entity entity = getEntity(event);
        if(entity != null) {
            return entity.getType();
        }
        EntityQuery query = (EntityQuery) event.getDataContext().getData("query");
        if(query != null) {
            return query.getEntityType();
        }
        return null;
    }

    protected Set<String> getSupportedEntityTypes() {
        return Collections.emptySet(); // empty means all
    }

    protected Set<String> getSupportedPlaces() {
        return Collections.emptySet(); // empty means all
    }

    protected abstract void actionPerformed(AnActionEvent event, Project project, Entity entity);

    protected void update(AnActionEvent event, Project project, Entity entity) {
    }

    private boolean isSupportedType(String entityType) {
        Set<String> entityTypes = getSupportedEntityTypes();
        return entityTypes.isEmpty() || entityTypes.contains(entityType);
    }

    private boolean isSupportedPlace(String place) {
        Set<String> places = getSupportedPlaces();
        return places.isEmpty() || places.contains(place);
    }
}
