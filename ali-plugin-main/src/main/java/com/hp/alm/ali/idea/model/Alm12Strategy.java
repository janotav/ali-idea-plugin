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
