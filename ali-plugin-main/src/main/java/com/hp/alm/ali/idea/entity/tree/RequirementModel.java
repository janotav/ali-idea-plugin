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
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.intellij.openapi.project.Project;

import java.util.Arrays;
import java.util.List;

public class RequirementModel extends HierarchicalEntityModel {

    public RequirementModel(Project project) {
        super(project, "requirement", false, true);
    }

    public List<Entity> queryForNodes(EntityQuery query) {
        query.addColumn("type-id", 75);
        return super.queryForNodes(query);
    }

    public List<Entity> getRootEntity() { // TODO: fetch from server?
        Entity root = new Entity(getEntityType(), 0);
        root.setProperty("name", "Requirements");
        root.setProperty("type-id", "1");
        return Arrays.asList(root);
    }
}
