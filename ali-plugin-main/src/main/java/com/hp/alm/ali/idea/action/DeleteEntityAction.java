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
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

import java.util.HashSet;
import java.util.Set;

public class DeleteEntityAction extends EntityAction {

    private static Set<String> allowedTypes;
    static {
        allowedTypes = new HashSet<String>();
        allowedTypes.add("defect");
        allowedTypes.add("requirement");
        allowedTypes.add("release-backlog-item"); // make sure we are visible in the EntityQuery("release-backlog-item") context
        allowedTypes.add("acceptance-test");
    }

    public DeleteEntityAction() {
        super("Delete", "Delete entity", IconLoader.getIcon("/actions/cancel.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return allowedTypes;
    }

    @Override
    protected boolean enabledPredicate(Project project, Entity entity) {
        EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        return !entityEditManager.isEditing(entity);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, final Entity entity) {
        EntityLabelService entityLabelService = project.getComponent(EntityLabelService.class);
        entityLabelService.loadEntityLabelAsync(entity.getType(), new AbstractCachingService.DispatchCallback<String>() {
            @Override
            public void loaded(String entityLabel) {
                if(Messages.showYesNoDialog("Do you really want to delete this " + entityLabel + "?", "Confirmation", null) == Messages.YES) {
                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            EntityService entityService = project.getComponent(EntityService.class);
                            entityService.deleteEntity(entity);
                        }
                    });
                }
            }
        });
    }
}
