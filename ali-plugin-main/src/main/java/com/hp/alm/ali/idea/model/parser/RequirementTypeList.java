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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class RequirementTypeList extends AbstractList<Entity> {

    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("type".equals(localPart)) {
            String subType = element.getAttributeByName(new QName(null, "name")).getValue();
            int id = Integer.valueOf(element.getAttributeByName(new QName(null, "id")).getValue());
            Entity entity = new Entity("requirement-type", id);
            entity.setProperty("name", subType);
            add(entity);
        }
    }

    public static RequirementTypeList create(InputStream is) {
        RequirementTypeList list = new RequirementTypeList();
        list.initNoEx(is);
        return list;
    }
}
