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

package com.hp.alm.ali.idea.entity;

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.EntityFields;
import com.hp.alm.ali.idea.content.detail.HasEntity;
import com.hp.alm.ali.idea.entity.edit.LockingStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityEditManager implements ProjectManagerListener, ContentManagerListener, AnActionListener, ServerTypeListener {

    private Map<Entity, Boolean> edited = new HashMap<Entity, Boolean>();
    private Project project;
    private AliProjectConfiguration configuration;
    private RestService restService;
    private LockingStrategy lockingStrategy;
    private boolean closeProjectAccepted;
    private boolean closeContentsCanceled;

    public EntityEditManager(Project project, AliProjectConfiguration configuration, ProjectManager projectManager, RestService restService, ActionManager actionManager) {
        this.project = project;
        this.configuration = configuration;
        this.restService = restService;
        projectManager.addProjectManagerListener(this);
        actionManager.addAnActionListener(this);
        restService.addServerTypeListener(this);
    }

    public synchronized boolean isEditing(Entity entity) {
        return edited.containsKey(entity);
    }

    public Entity startEditing(Entity entity) {
        synchronized (this) {
            if(edited.containsKey(entity)) {
                throw new IllegalArgumentException("Entity edit in progress");
            }
            edited.put(entity, false);
        }
        if(entity.getId() > 0) {
            Entity lock = lockingStrategy.lock(entity);
            synchronized (this) {
                edited.remove(entity);
                if(lock != null) {
                    edited.put(lock, false);
                }
            }
            return lock;
        } else {
            return entity;
        }
    }

    public void stopEditing(Entity entity) {
        if(entity.getId() > 0) {
            lockingStrategy.unlock(entity);
        }
        synchronized (this) {
            edited.remove(entity);
        }
    }

    public synchronized void setEntityDirty(Entity entity, boolean dirty) {
        if(!edited.containsKey(entity)) {
            throw new IllegalArgumentException("Entity edit not in progress");
        }
        edited.put(entity, dirty);
    }

    public List<String> getEditorFields(String entityType) {
        EntityFields fields = configuration.getFields(entityType);
        if(fields.getColumns().isEmpty()) {
            fields.setColumns(restService.getModelCustomization().getDefaultFields(entityType));
        }
        return fields.getColumns();
    }

    @Override
    public void projectOpened(Project project) {
    }

    @Override
    public boolean canCloseProject(Project project) {
        if(closeProjectAccepted) {
            return true;
        }
        synchronized (this) {
            if(!edited.values().contains(true)) {
                return true;
            }
        }
        closeProjectAccepted = askForApproval();
        return closeProjectAccepted;
    }

    public int askUser() {
        return Messages.showYesNoCancelDialog(project, "There are unsaved changes, if you proceed they will be discarded. Proceed?", "HP ALI", Messages.getQuestionIcon());
    }

    private boolean askForApproval() {
        return askUser() == Messages.YES;
    }

    @Override
    public void projectClosed(Project project) {
    }

    @Override
    public synchronized void projectClosing(Project project) {
        if(!edited.isEmpty()) {
            for(Entity entity: edited.keySet()) {
                lockingStrategy.unlock(entity);
            }
        }
    }

    @Override
    public void contentAdded(ContentManagerEvent contentManagerEvent) {
    }

    @Override
    public synchronized void contentRemoved(ContentManagerEvent contentManagerEvent) {
    }

    @Override
    public synchronized void contentRemoveQuery(ContentManagerEvent contentManagerEvent) {
        Entity entity = ((HasEntity)contentManagerEvent.getContent().getComponent()).getEntity();
        if(!edited.containsKey(entity))  {
            // not edited
            return;
        }
        if(!Boolean.TRUE.equals(edited.get(entity))) {
            // not dirty
            return;
        }
        if(closeContentsCanceled) {
            // dirty and canceled
            contentManagerEvent.consume();
            return;
        }
        switch (askUser()) {
            case Messages.YES:
                // approve
                return;

            case Messages.CANCEL:
                closeContentsCanceled = true;
        }
        // dirty and rejected or canceled
        contentManagerEvent.consume();
    }

    @Override
    public void selectionChanged(ContentManagerEvent contentManagerEvent) {
    }

    @Override
    public void beforeActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {
    }

    @Override
    public void afterActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {
        // we should identify the "close all" action and prevent unrelated actions from resetting the flag
        closeContentsCanceled = false;
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            lockingStrategy = restService.getModelCustomization().getLockingStrategy();
        } else {
            lockingStrategy = null;
        }
    }
}
