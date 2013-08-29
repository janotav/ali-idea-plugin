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

import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EntityEditStrategyImpl implements EntityEditStrategy {

    private static Set<String> entityTypes;
    static {
        entityTypes = new HashSet<String>();
        entityTypes.add("defect");
        entityTypes.add("requirement");
        entityTypes.add("build-instance");
        entityTypes.add("release-backlog-item");
    }

    protected Project project;
    protected EntityEditManager entityEditManager;

    public EntityEditStrategyImpl(Project project) {
        this.project = project;
        entityEditManager = project.getComponent(EntityEditManager.class);
    }

    @Override
    public boolean isEditable(String entityType) {
        return entityTypes.contains(entityType);
    }

    @Override
    public void executeEditor(EntityRef entity) {
        EntityEditor entityEditor = new EntityEditor(project, "Modify {0}", entity.toEntity(), entityEditManager.getEditorFields(entity.type), false, false, Collections.<String>emptyList(), new Edit(project));
        entityEditor.execute();
    }

    public static class Edit extends BaseEditor.BaseHandler {

        public Edit(Project project) {
            super(project);
        }

        @Override
        public boolean save(Entity modified, Entity base) {
            return entityService.updateEntity(modified, null, false, false, true) != null;
        }
    }
}
