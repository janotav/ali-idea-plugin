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

import com.hp.alm.ali.idea.model.parser.FieldList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Metadata {

//    private static Map<String, String> referenceMap;
//    static {
//        referenceMap = new HashMap<String, String>();
//        referenceMap.put("assign-rcyc", "release-cycle");
//        referenceMap.put("cycle-reference", "test-set");
//        referenceMap.put("test-set.parent-id", "test-set-folder");
//        referenceMap.put("build-detected", "build-instance");
//        referenceMap.put("build-closed", "build-instance");
//        referenceMap.put("build-instance.release", "release");
//        referenceMap.put("build-instance.type", "build-type");
//        referenceMap.put("defect.changeset", "changeset");
//    }

    private static Map<String, String> hierarchical;
    static {
        hierarchical = new HashMap<String, String>();
        hierarchical.put("release", "release-folder");
        hierarchical.put("release-cycle", "release");
        hierarchical.put("release-folder", "release-folder");
        hierarchical.put("test-set-folder", "test-set-folder");
        hierarchical.put("favorite", "favorite-folder");
        hierarchical.put("favorite-folder", "favorite-folder");
        hierarchical.put("requirement", "requirement");
    }

    private static Map<String, List<String>> revHierarchical;
    static {
        revHierarchical = new HashMap<String, List<String>>();
        for(String child: hierarchical.keySet()) {
            String parent = hierarchical.get(child);
            List<String> list = revHierarchical.get(parent);
            if(list == null) {
                list = new LinkedList<String>();
                revHierarchical.put(parent, list);
            }
            list.add(child);
        }
    }

    private static Map<String, String> hierarchicalPath;
    static  {
        hierarchicalPath = new HashMap<String, String>();
        hierarchicalPath.put("release-folder", "path");
        hierarchicalPath.put("test-set-folder", "hierarchical-path");
        hierarchicalPath.put("favorite-folder", "path");
        hierarchicalPath.put("requirement", "hierarchical-path");
    }

    private static Map<String, Integer> hierarchicalPathLength;
    static {
        hierarchicalPathLength = new HashMap<String, Integer>();
        hierarchicalPathLength.put("release-folder", 3);
        hierarchicalPathLength.put("test-set-folder", 3);
        hierarchicalPathLength.put("favorite-folder", 5);
        hierarchicalPathLength.put("requirement", 3);
    }

    private Map<String, Field> allFields;
    private List<Relation> relations;
    private String entityName;

    public Metadata(Project project, String entityName, boolean load) {
        this.allFields = new HashMap<String, Field>();
        this.relations = new ArrayList<Relation>();
        this.entityName = entityName;

        RestService restService = project.getComponent(RestService.class);

        if(load) {
            InputStream is = restService.getForStream("customization/entities/{0}/fields", entityName);
            for(Field field: FieldList.create(is)) {
                allFields.put(field.getName(), field);
            }

            relations.addAll(restService.getServerStrategy().getRelationList(entityName));
        }
    }

    public void add(Metadata metadata) {
        if(entityName.equals(metadata.entityName)) {
            allFields.putAll(metadata.getAllFields());
            relations.addAll(metadata.getRelations());
        } else {
            for(Field field: metadata.getAllFields().values()) {
                Field relatedField = field.cloneRelated(metadata.entityName);
                allFields.put(relatedField.getName(), relatedField);
            }
        }
    }

    private List<Relation> getRelations() {
        return relations;
    }

    public String getEntityType() {
        return entityName;
    }

    public static String getParentEntity(String entityType) {
        return hierarchical.get(entityType);
    }

    public static List<String> getChildEntity(String entityType) {
        return revHierarchical.get(entityType);
    }

    public static String getHierarchicalPathProperty(String entityType) {
        return hierarchicalPath.get(entityType);
    }

    public static Integer getHierarchicalPathLength(String entityType) {
        return hierarchicalPathLength.get(entityType);
    }

    public Map<String, Field> getAllFields() {
        return Collections.unmodifiableMap(allFields);
    }

    public List<Field> getRequiredFields() {
        LinkedList<Field> fields = new LinkedList<Field>();
        for(Field field: allFields.values()) {
            if(field.isRequired()) {
                fields.add(field);
            }
        }
        return fields;
    }

    public Field getField(String name) {
        return allFields.get(name);
    }

    public void removeField(String name) {
        allFields.remove(name);
    }

    public Map<String, List<Relation>> getRelationMap(boolean withUniqueAliasOnly) {
        HashMap<String, List<Relation>> map = new HashMap<String, List<Relation>>();
        for(Relation relation: relations) {
            if(withUniqueAliasOnly && relation.getAliases().isEmpty()) {
                continue;
            }
            List<Relation> list = map.get(relation.getTargetType());
            if(list == null) {
                list = new LinkedList<Relation>();
                map.put(relation.getTargetType(), list);
            }
            list.add(relation);
        }
        return map;
    }

    public void addField(Field field) {
        allFields.put(field.getName(), field);
    }
}
