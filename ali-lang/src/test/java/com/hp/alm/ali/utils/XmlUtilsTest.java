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

import junit.framework.Assert;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.StringReader;

public class XmlUtilsTest {

    @Test
    public void testCreateBasicInputFactory() throws Exception {
        XMLInputFactory factory = XmlUtils.createBasicInputFactory();
        factory.setXMLResolver(new XMLResolver() {
            @Override
            public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
                Assert.assertFalse("DTD should not be resolved.", "http://bogus.url/foo.dtd".equals(systemID));
                return null;
            }
        });
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader("<!DOCTYPE foo SYSTEM \"http://bogus.url/foo.dtd\"><xml/>"));
        while(reader.hasNext()) {
            reader.next();
        }
    }
}
