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
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

public class ModifyAction extends EntityAction {

    public ModifyAction() {
        super("Modify", "Modify entity", IconLoader.getIcon("/actions/editSource.png"));
    }

    @Override
    protected boolean visiblePredicate(Project project, String entityType) {
        RestService restService = project.getComponent(RestService.class);
        EntityEditStrategy entityEditStrategy = restService.getModelCustomization().getEntityEditStrategy();
        return entityEditStrategy.isEditable(entityType);
    }

    @Override
    protected boolean enabledPredicate(Project project, Entity entity) {
        EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        return !entityEditManager.isEditing(entity);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        RestService restService = project.getComponent(RestService.class);
        restService.getModelCustomization().getEntityEditStrategy().executeEditor(new EntityRef(entity));
    }
}
