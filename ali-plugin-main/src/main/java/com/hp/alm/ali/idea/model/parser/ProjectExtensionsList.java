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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class ProjectExtensionsList extends AbstractList<String[]> {

    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("Extension".equals(localPart)) {
            String name = element.getAttributeByName(new QName(null, "Name")).getValue();
            add(new String[2]);
            getLast()[0] = name;
        } else if("Version".equals(localPart)) {
            getLast()[1] = readNextValue();
        }
    }

    public static ProjectExtensionsList create(InputStream is) {
        ProjectExtensionsList extensionsList = new ProjectExtensionsList();
        extensionsList.initNoEx(is);
        return extensionsList;
    }
}
