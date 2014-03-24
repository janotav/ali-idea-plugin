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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.entity.tree.EntityNode;
import com.hp.alm.ali.idea.entity.tree.HierarchicalEntityModel;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class FilterableTree extends JTree implements Runnable {
    final private LinkedList<String> queue = new LinkedList<String>();
    private RestService restService;

    public FilterableTree(Project project, HierarchicalEntityModel treeModel) {
        super(treeModel);
        restService = project.getComponent(RestService.class);
    }

    public void setFilter(String filter) {
        synchronized (queue) {
            if(queue.isEmpty()) {
                ApplicationManager.getApplication().executeOnPooledThread(this);
            } else {
                queue.remove(0);
            }
            queue.add(filter);
        }
    }

    private EntityQuery handle(HierarchicalEntityModel model, List<Entity> selected) {
        Map<String, List<Entity>> missingParents = new HashMap<String, List<Entity>>();
        while(!selected.isEmpty()) {
            Entity entity = selected.remove(0);
            EntityNode node = model.getEntityNode(entity.getId(), entity.getType());
            if(node == null) {
                // not in our model yet
                int parentId = Integer.valueOf(entity.getPropertyValue("parent-id"));
                EntityNode parent = model.getEntityNode(parentId, model.getParentEntity(entity.getType()));
                if(parent == null) {
                    // have to wait until we resolve parent
                    String parentStr = String.valueOf(parentId);
                    List<Entity> todo = missingParents.get(parentStr);
                    if(todo == null) {
                        todo = new LinkedList<Entity>();
                        missingParents.put(parentStr, todo);
                    }
                    todo.add(entity);
                } else {
                    handleNode(model, parent, entity);
                    List<Entity> todo = missingParents.remove(String.valueOf(entity.getId()));
                    if(todo != null) {
                        // parent became known restart
                        selected.addAll(todo);
                    }
                }
            } else {
                // already known
                markVisible(node);
            }
        }
        if(!missingParents.isEmpty()) {
            for(List<Entity> todo: missingParents.values()) {
                selected.addAll(todo);
            }
            String entityType = selected.get(0).getType();
            EntityQuery parentQuery = new EntityQuery(model.getParentEntity(entityType));
            parentQuery.addColumn("parent-id", 75);
            String pathProperty = restService.getServerStrategy().getHierarchicalPathProperty(entityType);
            if(pathProperty != null) {
                // if hierarchical path is available search for all parents at once
                int length = Metadata.getHierarchicalPathLength(entityType);
                Set<String> parentPaths = new HashSet<String>();
                for(List<Entity> todo: missingParents.values()) {
                    for(Entity entity: todo) {
                        String pathValue = entity.getPropertyValue(pathProperty);
                        do {
                            pathValue = pathValue.replaceAll(".{"+length+"}$", "");
                            parentPaths.add(pathValue);
                        } while (pathValue.length() > length);
                    }
                }
                parentQuery.setValue(pathProperty, StringUtils.join(parentPaths.toArray(new String[0]), " or "));
            } else {
                // only search for immediate parent
                parentQuery.setValue("id", StringUtils.join(missingParents.keySet().toArray(new String[0]), " or "));
            }
            return parentQuery;
        } else {
            return null;
        }

    }

    private void handleNode(final HierarchicalEntityModel model, final EntityNode parent, Entity entity) {
        EntityNode node = new EntityNode(model, parent, entity);
        parent.addFilteredChild(node);
        markVisible(node);
    }

    public void markVisible(EntityNode node) {
        EntityNode parent = node.getParent();
        if(parent != null) {
            for(EntityNode p: parent.getPath()) {
                p.setChildMatching(true);
            }
        }
    }

    public void handle(HierarchicalEntityModel model, String entityType, String filter) {
        EntityQuery query = new EntityQuery(entityType);
        query.addColumn("parent-id", 75);
        query.setValue("name", "'*"+filter+"*'");

        LinkedList<List<Entity>> queue = new LinkedList<List<Entity>>();

        while(query != null) {
            List<Entity> todo = model.queryForNodes(query);
            query = handle(model, todo);
            if(!todo.isEmpty()) {
                // wait until we know parents
                queue.addFirst(todo);
            }
        }

        for(List<Entity> todo: queue) {
            for(Entity entity: todo) {
                if(model.getEntityNode(entity.getId(), entity.getType()) == null) {
                    int parentId = Integer.valueOf(entity.getPropertyValue("parent-id"));
                    EntityNode parent = model.getEntityNode(parentId, Metadata.getParentEntity(entity.getType()));
                    if(parent != null) {
                        handleNode(model, parent, entity);
                    }
                }
            }
        }
    }

    public void run() {
        final HierarchicalEntityModel model = (HierarchicalEntityModel) getModel();

        synchronized (model) {
            String filter;
            synchronized (queue) {
                filter = queue.removeFirst();
            }

            final Set<EntityNode> expanded = new HashSet<EntityNode>();
            for(EntityNode node: model.getNodes()) {
                if(isExpanded(new TreePath(node.getPath().toArray()))) {
                    expanded.add(node);
                }
            }

            model.setFilter(filter);
            model.clearChildFilteredFlag();

            if(!filter.isEmpty()) {
                model.getStatus().loading();
                try {
                    String entityType = model.getEntityType();
                    Set<String> processed = new HashSet<String>();
                    LinkedList<String> ordered = new LinkedList<String>();
                    while(entityType != null && processed.add(entityType)) {
                        ordered.addFirst(entityType); // parent first (may spare us from some missing-parent requests)
                        entityType = Metadata.getParentEntity(entityType);
                    }
                    for(String entity: ordered) {
                        handle(model, entity, filter);
                    }
                    model.getStatus().clear();
                } catch(Exception e) {
                    model.getStatus().info("Failed to load data", e, null);
                }
            }

            UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                public void run() {
                    List<EntityNode> toLoad = new LinkedList<EntityNode>();
                    for(EntityNode node: model.getNodes()) {
                        node.filterVisible();
                        TreePath path = new TreePath(node.getPath().toArray());
                        if(node.isChildMatching()) {
                            collapsePath(path); // workaround for expand not working sometimes (?!)
                            expandPath(path);
                        } else if(expanded.contains(node)) {
                            expandPath(path);
                        }
                        if(node.isIncomplete() && node.matchesFilter()) {
                            toLoad.add(node);
                        }
                    }
                    if(!toLoad.isEmpty()) {
                        model.lazyLoadChildren(toLoad, false);
                    }
                }
            });
        }
    }
}
