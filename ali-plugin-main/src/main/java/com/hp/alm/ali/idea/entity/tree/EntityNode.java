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
import com.hp.alm.ali.idea.model.Metadata;
import com.intellij.openapi.util.Pair;
import org.apache.commons.lang.StringUtils;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityNode implements TreeNode, Comparable<EntityNode> {
    private int ordering;
    private HierarchicalEntityModel model;
    private EntityNode parent;

    private Entity entity;
    private List<EntityNode> children;

    private List<EntityNode> filtered;
    private boolean childMatching;
    private boolean incomplete;
    private boolean loading;

    public EntityNode(HierarchicalEntityModel model, EntityNode parent, Entity entity) {
        this.model = model;
        this.parent = parent;
        this.entity = entity;

        model.addNode(this);
    }

    public void initChildren() {
        if(children == null) {
            children = new ArrayList<EntityNode>();
            filtered = new ArrayList<EntityNode>();
            model.lazyLoadChildren(Arrays.asList(this), true);
        }
    }

    public void storeChildren(List<EntityNode> children) {
        if(this.children == null) {
            this.children = new ArrayList<EntityNode>();
            this.filtered = new ArrayList<EntityNode>();
        }
        // TODO: keep sorted

        mergeChildren(children);

        this.children.clear();
        this.children.addAll(children);

        incomplete = false;
        loading = false;
    }

    public void filterVisible() {
        if(children != null) {
            mergeChildren(children);
        }
        model.nodeChanged(this);
    }

    private void mergeChildren(List<EntityNode> children) {
        for(int i = 0; i < this.filtered.size(); i++) {
            EntityNode child = this.filtered.get(i);
            if(!children.contains(child) || (!child.matchesFilter() && !child.isChildMatching())) {
                this.filtered.remove(i);
                model.nodesWereRemoved(this, new int[] { i }, new EntityNode[] { child });
                --i;
            }
        }
        for(EntityNode child: children) {
            if(!filtered.contains(child)) {
                if(child.matchesFilter() || child.isChildMatching()) {
                    int i = Collections.binarySearch(this.filtered, child);
                    this.filtered.add(-(i + 1), child);
                    model.nodesWereInserted(this, new int[] { -(i + 1) });
                }
            }
        }
    }

    public EntityNode getChildAt(int i) {
        initChildren();
        return filtered.get(i);
    }

    public int getChildCount() {
        initChildren();
        return filtered.size();
    }

    public EntityNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode treeNode) {
        initChildren();
        return filtered.indexOf(treeNode);
    }

    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    public boolean isLeaf() {
        List<String> childrenTypes = Metadata.getChildEntity(entity.getType());
        return childrenTypes == null || (model.getEntityType().equals(entity.getType()) && !childrenTypes.contains(entity.getType()));
    }

    public Enumeration children() {
        initChildren();
        return Collections.enumeration(getFiltered());
    }

    public Entity getEntity() {
        return entity;
    }

    public String toString() {
        String name = entity.getPropertyValue("name");
        if(loading) {
            name += "..."; // TODO: show as icon
        }
        String filter = model.getFilter();
        if(!filter.isEmpty()) {
            Matcher matcher = Pattern.compile(wildcardToRegex(filter), Pattern.CASE_INSENSITIVE).matcher(name);
            List<Pair<Integer, Integer>> list = new LinkedList<Pair<Integer, Integer>>();
            while(matcher.find()) {
                list.add(new Pair<Integer, Integer>(matcher.start(), matcher.end()));
            }
            if(!list.isEmpty()) {
                Collections.reverse(list);
                for(Pair<Integer, Integer> match: list) {
                    name = name.substring(0, match.first) + "<b>" + name.substring(match.first, match.second) + "</b>" + name.substring(match.second);
                }
                return "<html>" + name + "</html>";
            }
        }
        return name;
    }

    public List<EntityNode> getPath() {
        List<EntityNode> path;
        if(parent == null) {
            path = new ArrayList<EntityNode>();
        } else {
            path = parent.getPath();
        }
        path.add(this);
        return path;
    }

    public boolean addFilteredChild(EntityNode node) {
        if(children == null) {
            children = new ArrayList<EntityNode>();
            incomplete = true;
        }
        children.add(node);

        if(filtered == null) {
            filtered = new ArrayList<EntityNode>();
        }
        filtered.add(node);
        return incomplete;
    }

    public List<EntityNode> getFiltered() {
        return filtered;
    }

    public boolean matchesFilter() {
        if(parent != null && parent.matchesFilter()) {
            return true;
        } else {
            return model.getFilter().isEmpty() || containsIgnoreCase(entity.getPropertyValue("name"), model.getFilter());
        }
    }

    private boolean containsIgnoreCase(String str, String subStr) {
        return Pattern.compile(".*" + wildcardToRegex(subStr) + ".*", Pattern.CASE_INSENSITIVE).matcher(str).matches();
    }

    private String wildcardToRegex(String mask) {
        String[] tokens = mask.split("\\*", -1);
        for(int i = 0; i < tokens.length; i++) {
            tokens[i] = Pattern.quote(tokens[i]);
        }
        return StringUtils.join(tokens, ".*");
    }

    public boolean isChildMatching() {
        return childMatching;
    }

    public void setChildMatching(boolean childMatching) {
        this.childMatching = childMatching;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean b) {
        this.incomplete = b;
    }

    public int compareTo(EntityNode entityNode) {
        int ret = ordering - entityNode.ordering;
        if(ret == 0) {
            return getEntity().getPropertyValue("name").compareTo(entityNode.getEntity().getPropertyValue("name"));
        } else {
            return ret;
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }
}
