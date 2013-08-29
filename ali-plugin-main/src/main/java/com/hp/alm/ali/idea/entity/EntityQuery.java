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

package com.hp.alm.ali.idea.entity;

import com.hp.alm.ali.idea.model.Metadata;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import javax.swing.SortOrder;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityQuery extends AbstractEntityFilter<EntityQuery> {

    public static Comparator<EntityQuery> COMPARATOR_NAME = new Comparator<EntityQuery>() {
        public int compare(EntityQuery filter1, EntityQuery filter2) {
            return filter1.getName().compareTo(filter2.getName());
        }
    };

    private LinkedHashMap<String, SortOrder> order = new LinkedHashMap<String, SortOrder>();
    private LinkedHashMap<String, Integer> view = new LinkedHashMap<String, Integer>();
    private String name;
    private EntityRef parentRef;
    private Map<String, EntityCrossFilter> cross = new HashMap<String, EntityCrossFilter>();
    private Integer pageSize;
    private Integer startIndex;

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized String getName() {
        return name;
    }

    public EntityQuery(String entityType) {
        super(entityType);
    }

    public synchronized boolean setValue(String entityType, String prop, String value) {
        if(entityType == null || entityType.equals(this.entityType)) {
            return setValue(prop, value);
        } else {
            return getCrossFilter(entityType).setValue(prop, value);
        }
    }

    public synchronized boolean setOrValues(String prop, List<String> values) {
        return setValue(prop, StringUtils.join(values.toArray(new String[values.size()]), " OR "));
    }

    public synchronized void setOrder(LinkedHashMap<String, SortOrder> order) {
        this.order.clear();
        this.order.putAll(order);
    }

    public synchronized LinkedHashMap<String, SortOrder> getOrder() {
        return new LinkedHashMap<String, SortOrder>(order);
    }

    public synchronized LinkedHashMap<String, Integer> getColumns() {
        return new LinkedHashMap<String, Integer>(view);
    }

    public synchronized void clear() {
        super.clear();
        view.clear();
        order.clear();
        cross.clear();
        resolvedFilters.clear();
    }

    public static String encode(String val) {
        try {
            return URLEncoder.encode(val, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return val;
        }
    }

    public static String decode(String val) {
        try {
            return URLDecoder.decode(val, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return val;
        }
    }

    public synchronized void addColumn(String name, int width) {
        view.put(name, width);
    }

    public synchronized void setColumns(LinkedHashMap<String, Integer> map) {
        view.clear();
        view.putAll(map);
    }

    public synchronized void removeView(String name) {
        view.remove(name);
    }

    public synchronized void addOrder(String name, SortOrder dir) {
        order.put(name, dir);
    }

    public synchronized void removeOrder(String name) {
        order.remove(name);
    }

    public synchronized EntityQuery clone() {
        EntityQuery copy = new EntityQuery(getEntityType());
        copy.copyFrom(this);
        return copy;
    }

    public synchronized void copyFrom(EntityQuery filter) {
        synchronized (filter) {
            super.copyFrom(filter);
            setParent(filter.getParent());
            setOrder(filter.getOrder());
            setColumns(filter.getColumns());
            resolvedFilters.addAll(filter.resolvedFilters);
            setPageSize(filter.getPageSize());
            setStartIndex(filter.getStartIndex());

            Map<String, EntityCrossFilter> existing = new HashMap<String, EntityCrossFilter>(cross);
            for(String alias: filter.cross.keySet()) {
                EntityCrossFilter crossFilter = filter.cross.get(alias);
                EntityCrossFilter ex = existing.remove(alias);
                if(ex == null) {
                    // new cross-filter
                    cross.put(alias, crossFilter.clone());
                } else {
                    // update existing
                    ex.copyFrom(crossFilter);
                }
            }
            // remove those not present in source filter
            cross.keySet().removeAll(existing.keySet());
        }
    }

    public synchronized Element toElement(String targetName) {
        Element element = new Element(targetName);
        if(getName() != null) {
            element.setAttribute("name", getName());
        }
        Element filter = super.toElement("filter");
        element.addContent(filter);
        Element cross = new Element("crossFilter");
        Map<String, EntityCrossFilter> crossFilters = getCrossFilters();
        for(String alias: crossFilters.keySet()) {
            EntityCrossFilter f = crossFilters.get(alias);
            Element crossElement = f.toElement(f.getEntityType());
            if(!alias.equals(f.getEntityType())) {
                crossElement.setAttribute("alias", alias);
            }
            cross.addContent(crossElement);
        }
        element.addContent(cross);
        Element orderBy = new Element("order");
        for(String name: order.keySet()) {
            Element child = new Element(name);
            child.setAttribute("dir", order.get(name).name());
            orderBy.addContent(child);
        }
        element.addContent(orderBy);
        Element viewing = new Element("view");
        for(String name: view.keySet()) {
            Element child = new Element("column");
            child.setAttribute("name", name);
            child.setAttribute("width", String.valueOf(view.get(name)));
            viewing.addContent(child);
        }
        element.addContent(viewing);
        return element;
    }

    public synchronized void fromElement(Element element) {
        setName(element.getAttributeValue("name"));
        Element filter = element.getChild("filter");
        if(filter != null) {
            super.fromElement(filter);
        }
        Element cross = element.getChild("crossFilter");
        if(cross != null) {
            this.cross.clear();
            for(Element child: (List<Element>)cross.getChildren()) {
                EntityCrossFilter cf = new EntityCrossFilter(child.getName());
                cf.fromElement(child);
                String alias = child.getAttributeValue("alias", child.getName());
                this.cross.put(alias, cf);
            }
        }
        Element orderBy = element.getChild("order");
        if(orderBy != null) {
            order.clear();
            for(Element child: (List<Element>)orderBy.getChildren()) {
                order.put(child.getName(), SortOrder.valueOf(child.getAttributeValue("dir")));
            }
        }
        Element viewing = element.getChild("view");
        if(viewing != null) {
            view.clear();
            for(Element child: (List<Element>)viewing.getChildren("column")) {
                view.put(child.getAttributeValue("name"), Integer.valueOf(child.getAttributeValue("width")));
            }
        }
    }

    public synchronized EntityCrossFilter getCrossFilter(String entityType) {
        return getCrossFilter(entityType, entityType);
    }

    public synchronized EntityCrossFilter getCrossFilter(String entityType, String alias) {
        EntityCrossFilter crossFilter = cross.get(alias);
        if(crossFilter == null) {
            crossFilter = new EntityCrossFilter(entityType);
            cross.put(alias, crossFilter);
        }
        return crossFilter;
    }

    public synchronized Map<String, EntityCrossFilter> getCrossFilters() {
        HashMap<String, EntityCrossFilter> map = new HashMap<String, EntityCrossFilter>();
        for(String alias: cross.keySet()) {
            EntityCrossFilter filter = cross.get(alias);
            if(!filter.isEmpty()) {
                map.put(alias, filter);
            }
        }
        return map;
    }

    public synchronized boolean isEmpty() {
        if(!super.isEmpty()) {
            return false;
        }
        if(!order.isEmpty()) {
            return false;
        }
        for(EntityCrossFilter crossFilter: cross.values()) {
            if(!crossFilter.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public synchronized void purgeInvalid(Metadata metadata) {
        Set<String> allFields = metadata.getAllFields().keySet();

        props.keySet().retainAll(allFields);
        order.keySet().retainAll(allFields);
        view.keySet().retainAll(allFields);
    }

    public void setParent(EntityRef parent) {
        this.parentRef = parent;
    }

    public EntityRef getParent() {
        return parentRef;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }
}
