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
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Set;

public class RefreshAction extends EntityAction {

    public RefreshAction() {
        super("Refresh", "Refresh entity information", IconLoader.getIcon("/actions/sync.png"));
    }

    @Override
    protected Set<String> getSupportedPlaces() {
        return Collections.singleton("detail");
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        EntityService entityService = project.getComponent(EntityService.class);
        entityService.refreshEntity(new EntityRef(entity));
    }
}
