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

package com.hp.alm.ali.idea.entity.edit;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.AttachmentService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

public class DummyLock implements LockingStrategy {

    protected EntityService entityService;
    protected AttachmentService attachmentService;
    protected RestService restService;

    public DummyLock(Project project) {
        this.entityService = project.getComponent(EntityService.class);
        this.attachmentService = project.getComponent(AttachmentService.class);
        this.restService = project.getComponent(RestService.class);
    }

    @Override
    public Entity lock(Entity entity) {
        if("attachment".equals(entity.getType())) {
            return attachmentService.getAttachmentEntity(entity);
        } else if("defect-link".equals(entity.getType()) && restService.getServerStrategy().hasSecondLevelDefectLink()) {
            return entityService.getDefectLink(Integer.valueOf(entity.getPropertyValue("first-endpoint-id")), Integer.valueOf(entity.getPropertyValue("id")));
        } else {
            return entityService.getEntity(new EntityRef(entity));
        }
    }

    @Override
    public void unlock(Entity entity) {
    }
}
