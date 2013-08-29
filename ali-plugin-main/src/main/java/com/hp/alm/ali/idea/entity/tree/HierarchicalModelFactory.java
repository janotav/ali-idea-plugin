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

import com.intellij.openapi.project.Project;

public class HierarchicalModelFactory {
    private Project project;

    public HierarchicalModelFactory(Project project) {
        this.project = project;
    }

    public HierarchicalEntityModel createModel(String entityType) {
        if("test-set-folder".equals(entityType)) {
            return new TestSetFolderModel(project);
        } else if("requirement".equals(entityType)) {
            return new RequirementModel(project);
        } else {
            return new HierarchicalEntityModel(project, entityType, false, true);
        }
    }
}
