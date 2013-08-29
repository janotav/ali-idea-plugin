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

import com.hp.alm.ali.idea.model.Relation;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class RelationList extends AbstractList<Relation> {

    public static RelationList create(InputStream is) {
        try {
            return new RelationList(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private String targetEntity;
    private String relationName;

    private RelationList(InputStream is) throws XMLStreamException {
        init(is);
    }

    @Override
    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("TargetEntity".equals(localPart)) {
            targetEntity = readNextValue();
            add(new Relation(relationName, targetEntity));
        } else if("Alias".equals(localPart)) {
            if(Boolean.parseBoolean(element.getAttributeByName(new QName(null, "Unique")).getValue())) {
                String name = element.getAttributeByName(new QName(null, "Name")).getValue();
                getLast().addAlias(name);
            }
        } else if("Relation".equals(localPart)) {
            relationName = element.getAttributeByName(new QName(null, "Name")).getValue();
        }
    }
}
