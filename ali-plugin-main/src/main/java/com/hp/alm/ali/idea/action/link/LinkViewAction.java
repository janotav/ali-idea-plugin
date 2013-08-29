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
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Set;

public class LinkViewAction extends EntityAction {

    public LinkViewAction() {
        super("View", "View linked entity", IconLoader.getIcon("/actions/preview.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("defect-link");
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        openLinkedEntity(project, entity, (Entity)event.getDataContext().getData("master-entity"));
    }

    public static void openLinkedEntity(Project project, Entity linkEntity, Entity master) {
        EntityService entityService = project.getComponent(EntityService.class);
        Entity defectLink = entityService.getDefectLink(Integer.valueOf(linkEntity.getPropertyValue("first-endpoint-id")), linkEntity.getId());
        EntityRef entityRef;
        if(defectLink.getPropertyValue("first-endpoint-id").equals(master.getPropertyValue("id")) && "defect".equals(master.getType())) {
            entityRef = new EntityRef(defectLink.getPropertyValue("second-endpoint-type"), Integer.valueOf(defectLink.getPropertyValue("second-endpoint-id")));
        } else {
            entityRef = new EntityRef("defect", Integer.valueOf(defectLink.getPropertyValue("first-endpoint-id")));
        }
        AliContentFactory.loadDetail(project, entityRef.toEntity(), true, true);
    }
}
