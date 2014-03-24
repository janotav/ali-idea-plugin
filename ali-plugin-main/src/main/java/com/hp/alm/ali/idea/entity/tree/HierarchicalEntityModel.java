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

import com.hp.alm.ali.idea.entity.DummyStatusIndicator;
import com.hp.alm.ali.idea.model.StatusIndicator;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HierarchicalEntityModel extends DefaultTreeModel {
    protected RestService restService;
    protected EntityService entityService;
    protected StatusIndicator status;
    protected Project project;
    private String entityType;
    final private Map<String, EntityNode> map;
    private String filter = "";
    private boolean multiRoot;

    public HierarchicalEntityModel(Project project, String entityType, boolean multiRoot, boolean initRoot) {
        super(new DefaultMutableTreeNode());

        this.project = project;
        this.entityType = entityType;
        this.restService = project.getComponent(RestService.class);
        this.entityService = project.getComponent(EntityService.class);
        this.multiRoot = multiRoot;
        map = Collections.synchronizedMap(new HashMap<String, EntityNode>());

        status = new DummyStatusIndicator();

        if(initRoot) {
            initRootEntity();
        }
    }

    protected void initRootEntity() {
        List<Entity> roots = getRootEntity();
        if(!roots.isEmpty()) {
            if(roots.size() == 1 && !multiRoot) {
                setRoot(new EntityNode(this, null, roots.get(0)));
            } else {
                Entity root = new Entity(roots.get(0).getType(), -1);
                root.setProperty("name", "Root");
                setRoot(new EntityNode(this, null, root));
            }
        }
    }


    public String getParentEntity(String entityType) {
        return Metadata.getParentEntity(entityType);
    }

    public List<Entity> getRootEntity() {
        String rootEntityType = entityType;
        while(true) {
            String parentEntityType = Metadata.getParentEntity(rootEntityType);
            if(parentEntityType.equals(rootEntityType)) {
                if("release-folder".equals(rootEntityType)) {
                    Entity entity = new Entity(rootEntityType, 1);
                    entity.setProperty("name", "Releases");
                    return Arrays.asList(entity);
                } else {
                    Entity entity = new Entity(rootEntityType, 0);
                    entity.setProperty("name", "Root");
                    return Arrays.asList(entity);
                }
            }
            rootEntityType = parentEntityType;
        }
    }

    public void addNode(EntityNode entityNode) {
        Entity entity = entityNode.getEntity();
        map.put(entity.getType()+"."+entity.getId(), entityNode);
    }

    public String getEntityType() {
        return entityType;
    }

    public EntityNode getEntityNode(int id, String entityType) {
        return map.get(entityType+"."+id);
    }

    public void loadChildren(Map<EntityNode, List<EntityNode>> parentToChildren, List<EntityNode> parents) {
        Map<String, List<String>> childrenEntity = new HashMap<String, List<String>>();
        for(EntityNode parent: parents) {
            parentToChildren.put(parent, new LinkedList<EntityNode>());
            List<String> childrenOfParentType = getChildrenEntity(parent.getEntity().getType());
            for(String childEntityType: childrenOfParentType) {
                List<String> list = childrenEntity.get(childEntityType);
                if(list == null) {
                    list = new LinkedList<String>();
                    childrenEntity.put(childEntityType, list);
                }
                list.add(parent.getEntity().getPropertyValue("id"));
            }
        }
        for(String childEntity: childrenEntity.keySet()) {
            String parentEntityType = Metadata.getParentEntity(childEntity);

            List<Entity> entities = queryForChildren(childEntity, childrenEntity.get(childEntity));
            for(Entity child: entities) {
                EntityNode parent = getEntityNode(Integer.valueOf(child.getPropertyValue("parent-id")), parentEntityType);
                EntityNode node = getEntityNode(child.getId(), child.getType());
                if(node == null) {
                    node = new EntityNode(this, parent, child);
                }
                parentToChildren.get(parent).add(node);
            }
        }
    }

    private List<Entity> queryForChildren(String entityType, List<String> parentIds) {
        EntityQuery query = new EntityQuery(entityType);
        query.addColumn("parent-id", 75);
        query.setValue("parent-id", StringUtils.join(parentIds.toArray(new String[0]), " or "));
        return queryForNodes(query);
    }

    public List<Entity> queryForNodes(EntityQuery query) {
        query.addColumn("id", 75);
        query.addColumn("name", 75);
        String pathProperty = restService.getServerStrategy().getHierarchicalPathProperty(query.getEntityType());
        if(pathProperty != null) {
            query.addColumn(pathProperty, 75);
        }
        return entityService.query(query);
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public void clearChildFilteredFlag() {
        for(EntityNode node: map.values()) {
            node.setChildMatching(false);
        }
    }

    public Collection<EntityNode> getNodes() {
        synchronized (map) {
            return new LinkedList<EntityNode>(map.values());
        }
    }

    public void lazyLoadChildren(final List<EntityNode> parents, final boolean showMessage) {
        for(EntityNode parent: parents) {
            parent.setIncomplete(false);
            if(showMessage) {
                parent.setLoading(true);
                nodeChanged(parent);
            }
        }
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                synchronized (HierarchicalEntityModel.this) {
                    status.loading();
                    final Map<EntityNode, List<EntityNode>> children = new HashMap<EntityNode, List<EntityNode>>();
                    loadChildren(children, parents);
                    status.clear();
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        public void run() {
                            for(EntityNode parent: children.keySet()) {
                                parent.storeChildren(children.get(parent));
                                if(showMessage) {
                                    nodeChanged(parent);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public List<String> getChildrenEntity(String entityType) {
        List<String> childEntity = Metadata.getChildEntity(entityType);
        if(childEntity == null) {
            return Collections.emptyList();
        } else if(this.entityType.equals(entityType)) {
            if(childEntity.contains(entityType)) {
                return Arrays.asList(entityType);
            } else {
                return Collections.emptyList();
            }
        } else {
            return childEntity;
        }
    }

    public void setStatus(StatusIndicator status) {
        this.status = status;
    }

    public StatusIndicator getStatus() {
        return status;
    }
}
