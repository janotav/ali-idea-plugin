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

package com.hp.alm.ali.utils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

public class XmlUtils {

    /**
     * Creates XMLInputFactory with DTD support disabled.
     * @return xml input factory
     */
    public static XMLInputFactory createBasicInputFactory() {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return xmlFactory;
    }
    
    public static String getAttrValue(StartElement element, String attr) {
        return element.getAttributeByName(new QName(attr)).getValue();
    }
    
    public static String getElementLocalName(StartElement element) {
        return element.getName().getLocalPart();
    }
    
    public static String getElementLocalName(EndElement element) {
        return element.getName().getLocalPart();
    }

    public static String readElementContent(XMLEventReader reader) throws XMLStreamException {
        return ((Characters) reader.nextEvent()).getData();
    }
}