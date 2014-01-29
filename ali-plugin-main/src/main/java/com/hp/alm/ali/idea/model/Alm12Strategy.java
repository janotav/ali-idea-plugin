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

package com.hp.alm.ali.idea.model;

import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

public class Alm12Strategy extends ApolloStrategy {
    private static Map<String, String> developmentAliasMap;
    {
        developmentAliasMap = new HashMap<String, String>();
        developmentAliasMap.put("defect", "connected-to-defect");
        developmentAliasMap.put("requirement", "connected-to-requirement");
        developmentAliasMap.put("build-instance", "build-instance");
    }

    private static Map<String, String> hierarchicalPath;
    static  {
        hierarchicalPath = new HashMap<String, String>();
        hierarchicalPath.put("release-folder", "hierarchical-path");
        hierarchicalPath.put("test-set-folder", "hierarchical-path");
        hierarchicalPath.put("favorite-folder", "path");
        hierarchicalPath.put("requirement", "hierarchical-path");
    }

    public Alm12Strategy(Project project, RestService restService) {
        super(project, restService);
    }

    @Override
    public String getHierarchicalPathProperty(String entityType) {
        return hierarchicalPath.get(entityType);
    }

    @Override
    public String getDevelopmentAlias(String entity) {
        return developmentAliasMap.get(entity);
    }

}
