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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.type.UserType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class FieldList extends AbstractList<Field> {

    public static FieldList create(InputStream is) {
        try {
            return new FieldList(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private FieldList(InputStream is) throws XMLStreamException {
        init(is);
    }

    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("Field".equals(localPart)) {
            String name = element.getAttributeByName(new QName(null, "Name")).getValue();
            Attribute attr = element.getAttributeByName(new QName(null, "Label"));
            String label = attr != null? attr.getValue(): name; // requirement.req-ver-stamp has no label
            add(new Field(name, label));
        } else if("Type".equals(localPart)) {
            Field field = getLast();
            String val = readNextValue(reader);
            if("Number".equals(val)) {
                field.setClazz(Integer.class);
            } else if("UsersList".equals(val)) {
                field.setClazz(UserType.class);
            } else if("Memo".equals(val)) {
                field.setBlob(true);
            }
        } else if("List-Id".equals(localPart)) {
            int listId = Integer.valueOf(AbstractList.readNextValue(reader));
            if(listId == 0) {
                // ALM 12.00.6489.0 deployed in Center returns zero for defect summary field
                return;
            }
            getLast().setListId(listId);
        } else if("Editable".equals(localPart)) {
            getLast().setEditable(Boolean.valueOf(AbstractList.readNextValue(reader)));
        } else if("Filterable".equals(localPart)) {
            getLast().setCanFilter(Boolean.valueOf(AbstractList.readNextValue(reader)));
        } else if("Required".equals(localPart)) {
            getLast().setRequired(Boolean.valueOf(AbstractList.readNextValue(reader)));
        } else if("References".equals(localPart)) {
            Attribute referenceTypeField = element.getAttributeByName(new QName(null, "ReferenceTypeField"));
            if(referenceTypeField != null) {
                getLast().setReferencedTypeField(referenceTypeField.getValue());
            }
        } else if("RelationReference".equals(localPart) && getLast().getReferencedTypeField() == null) {
            getLast().setReferencedType(element.getAttributeByName(new QName(null, "ReferencedEntityType")).getValue());
        }
    }
}
