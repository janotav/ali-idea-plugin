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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.parser.AbstractList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectListService extends AbstractCachingService<String, Map<Integer, List<String>>, AbstractCachingService.Callback<Map<Integer, List<String>>>> {

    private RestService restService;

    public ProjectListService(Project project, RestService restService) {
        super(project);
        this.restService = restService;
    }

    public List<String> getProjectList(String entityType, Field field) {
        String relatedType = field.getRelatedType();
        if(relatedType != null) {
            return getValue(relatedType).get(field.getListId());
        } else {
            return getValue(entityType).get(field.getListId());
        }
    }

    @Override
    protected Map<Integer, List<String>> doGetValue(String entityType) {
        Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
        try {
            InputStream is = restService.getForStream("customization/entities/{0}/lists", entityType);
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader reader = factory.createXMLEventReader(is);
            int listId = 0;
            while(true) {
                XMLEvent event = reader.nextEvent();
                if(event instanceof EndDocument) {
                    reader.close();
                    break;
                }
                if(event instanceof StartElement) {
                    String localPart = ((StartElement) event).getName().getLocalPart();
                    if("Id".equals(localPart)) {
                        listId = Integer.valueOf(AbstractList.readNextValue(reader));
                        map.put(listId, new ArrayList<String>());
                    } else if("Item".equals(localPart)) {
                        String value = ((StartElement) event).getAttributeByName(new QName(null, "value")).getValue();
                        map.get(listId).add(value);
                    }
                }
            }
        } catch(XMLStreamException e) {
            // return what was read so far
        }
        return map;
    }
}
