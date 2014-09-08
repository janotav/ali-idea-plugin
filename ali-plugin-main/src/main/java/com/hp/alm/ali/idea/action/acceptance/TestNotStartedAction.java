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

package com.hp.alm.ali.idea.action.acceptance;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.EntityService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Set;

public class TestNotStartedAction extends EntityAction {

    public TestNotStartedAction() {
        super("Not Started", "Acceptance Test Not Started", IconLoader.getIcon("/debugger/threadStates/paused.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("acceptance-test");
    }

    @Override
    protected void update(AnActionEvent event, Project project, Entity entity) {
        boolean notStarted = "Not Started".equals(entity.getPropertyValue("status"));
        event.getPresentation().setEnabled(!notStarted);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        entity.setProperty("status", "Not Started");
        project.getComponent(EntityService.class).updateEntity(entity, Collections.singleton("status"), false);
    }
}
