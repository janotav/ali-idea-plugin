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

import com.hp.alm.ali.idea.model.Audit;
import com.hp.alm.ali.idea.tasks.HpAlmTask;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class AuditList extends AbstractList<Audit> {

    private String propertyLabel;
    private String oldValue;
    private String newValue;

    public static AuditList create(InputStream is) {
        try {
            return new AuditList(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private AuditList(InputStream is) throws XMLStreamException {
        add(new Audit());
        init(is);
        if(!getLast().hasProperties()) {
            remove(size() - 1);
        }
    }

    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("Audit".equals(localPart)) {
            Audit audit = getLast();
            if(audit.hasProperties()) {
                add(new Audit());
            }
        } else if("Time".equals(localPart)) {
            getLast().setTime(HpAlmTask.parseDate(readNextValue()));
        } else if("User".equals(localPart)) {
            getLast().setUsername(readNextValue());
        } else if("Property".equals(localPart)) {
            this.propertyLabel = element.getAttributeByName(new QName(null, "Label")).getValue();
        } else if("OldValue".equals(localPart)) {
            this.oldValue = readNextValue();
        } else if("NewValue".equals(localPart)) {
            this.newValue = readNextValue();
        }
    }

    protected void onEndElement(EndElement element) {
        String localPart = element.getName().getLocalPart();

        if("Property".equals(localPart)) {
            getLast().addProperty(propertyLabel, oldValue, newValue);
        }
    }
}
