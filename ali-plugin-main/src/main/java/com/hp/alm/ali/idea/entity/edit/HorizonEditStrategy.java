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

import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

public class HorizonEditStrategy extends EntityEditStrategyImpl {

    public HorizonEditStrategy(Project project) {
        super(project);
    }

    @Override
    public void executeEditor(EntityRef entity) {
        HorizonEdit horizonEdit = new HorizonEdit(project);
        EntityEditor entityEditor = new EntityEditor(project, "Modify {0}", entity.toEntity(), entityEditManager.getEditorFields(entity.type), false, false, Collections.<String>emptyList(), horizonEdit);
        horizonEdit.setEditor(entityEditor);
        entityEditor.execute();
    }

    public static class HorizonEdit extends BaseEditor.BaseHandler {

        private EntityEditor editor;

        public HorizonEdit(Project project) {
            super(project);
        }

        @Override
        public boolean save(Entity modified, Entity baseEntity) {
            Entity primaryEntity = modified.getPrimaryEntity();
            if(!primaryEntity.isInitialized("id")) {
                primaryEntity.setProperty("id", baseEntity.getProperty("id"));
            }
            List<Entity> relatedEntities = modified.getRelatedEntities();
            Entity result = baseEntity.clone();
            boolean updated = false;
            boolean success = true;
            if(!new Entity(primaryEntity.getType(), primaryEntity.getId()).matches(primaryEntity)) {
                // only update if something changed (at least one property is specified)
                Entity updatedEntity = entityService.updateEntity(primaryEntity, null, false, false, false);
                if(updatedEntity == null) {
                    // don't update related if primary couldn't be updated
                    return false;
                }
                editor.refresh(updatedEntity, true);
                result.mergeEntity(updatedEntity);
                updated = true;
            }
            boolean backlogItemUpdated = false;
            for(Entity related: relatedEntities) {
                if(!related.isInitialized("id")) {
                    related.setProperty("id", baseEntity.getPropertyValue(related.getType() + ".id"));
                }
                Entity updatedEntity = entityService.updateEntity(related, null, false, false, true);
                if(updatedEntity != null) {
                    result.mergeRelatedEntity(updatedEntity);
                    updated = true;
                    if("release-backlog-item".equals(updatedEntity.getType())) {
                        backlogItemUpdated = true;
                    }
                } else {
                    result.mergeRelatedEntity(entityService.getEntity(new EntityRef(related)));
                    editor.refresh(result, false);
                    success = false;
                }
            }
            if(updated) {
                entityService.fireEntityLoaded(result, EntityListener.Event.GET);
                if(success && !backlogItemUpdated) {
                    // e.g. when changing defect status we want the backlog item to be updated in the taskboard
                    entityService.getEntityAsync(new EntityRef("release-backlog-item", Integer.valueOf(baseEntity.getPropertyValue("release-backlog-item.id"))), null);
                }
            }
            return success;
        }

        public void setEditor(EntityEditor editor) {
            this.editor = editor;
        }
    }
}
