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

package com.hp.alm.ali.idea.entity.tree;

import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.Map;

public class TestSetFolderModel extends HierarchicalEntityModel {

    private static final int ID_UNASSIGNED = -1;

    public TestSetFolderModel(Project project) {
        super(project, "test-set-folder", false, true);
    }

    public void loadChildren(Map<EntityNode, List<EntityNode>> parentToChildren, List<EntityNode> parents) {
        super.loadChildren(parentToChildren, parents);

        for(EntityNode parent: parents) {
            if(parent.getEntity().getId() == 0) {
                Entity unassigned = new Entity(getEntityType(), ID_UNASSIGNED);
                unassigned.setProperty("name", "Unassigned");
                parentToChildren.get(parent).add(new Unassigned(this, parent, unassigned));
            }
        }
    }

    public static class Unassigned extends EntityNode {

        public Unassigned(HierarchicalEntityModel model, EntityNode parent, Entity entity) {
            super(model, parent, entity);

            setOrdering(-1);
        }

        public boolean isLeaf() {
            return true;
        }
    }
}
