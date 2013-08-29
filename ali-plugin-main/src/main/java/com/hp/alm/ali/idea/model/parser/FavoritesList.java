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

package com.hp.alm.ali.idea.model.parser;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;

import javax.swing.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class FavoritesList extends AbstractList<Entity> {

    private String entityType;
    private String crossEntity = null;

    public static FavoritesList create(InputStream is, String entityType) {
        try {
            return new FavoritesList(is, entityType);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private FavoritesList(InputStream is, String entityType) throws XMLStreamException {
        this.entityType = entityType;
        init(is);
    }

    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("Favorite".equals(localPart)) {
            Entity entity = new Entity("favorite");
            entity.setProperty("query", new EntityQuery(entityType));
            add(entity);
            crossEntity = null;
        } else if("id".equals(localPart) || "parent-id".equals(localPart)) {
            getLast().setProperty(localPart, readNextValue());
        } else if("name".equals(localPart)) {
            String favoriteName = readNextValue();
            getLast().setProperty(localPart, favoriteName);
            getCurrentQuery().setName(favoriteName);
        } else if("CrossFilter".equals(localPart)) {
            crossEntity = element.getAttributeByName(new QName(null, "Entity")).getValue();
            getCurrentQuery().getCrossFilter(crossEntity).setInclusive(Boolean.valueOf(element.getAttributeByName(new QName(null, "Inclusive")).getValue()));

        } else if("Field".equals(localPart)) {
            EntityQuery filter = getCurrentQuery();

            String name = element.getAttributeByName(new QName(null, "Name")).getValue();
            Attribute valueAttr = element.getAttributeByName(new QName(null, "Value"));
            Attribute directionAttr = element.getAttributeByName(new QName(null, "Direction"));
            if(valueAttr != null) {
                // where part
                String value = valueAttr.getValue();
                if(crossEntity != null) {
                    filter.getCrossFilter(crossEntity).setValue(name, value);
                } else {
                    filter.setValue(name, value);
                }
            } else if(directionAttr != null) {
                // order by part
                String dir = directionAttr.getValue();
                LinkedHashMap<String, SortOrder> cols = filter.getOrder();
                cols.put(name, "Ascending".equalsIgnoreCase(dir) ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                filter.setOrder(cols);
            }
        } else if("Name".equals(localPart)) {
            // view part (1)
            EntityQuery filter = getCurrentQuery();

            String value = readNextValue();
            LinkedHashMap<String,Integer> cols = filter.getColumns();
            cols.put(value, 70);
            filter.setColumns(cols);

        } else if("Width".equals(localPart)) {
            // view part (2)
            EntityQuery filter = getCurrentQuery();

            String width = readNextValue();
            LinkedHashMap<String, Integer> cols = filter.getColumns();
            new LinkedList<Map.Entry<String, Integer>>(cols.entrySet()).getLast().setValue(Integer.valueOf(width));
            filter.setColumns(cols);
        }
    }

    private EntityQuery getCurrentQuery() {
        return ((EntityQuery)getLast().getProperty("query"));
    }
}
