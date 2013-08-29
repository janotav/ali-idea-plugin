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

package com.hp.alm.ali.idea.cfg;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "APMCommonSettings")
@XmlAccessorType(XmlAccessType.FIELD)
public class APMCommonSettings {

    @XmlElement(name = "Property")
    @XmlElementWrapper(name = "Properties")
    private List<Property> properties = new LinkedList<Property>();

    public List<String> getValues(String property) {
        for(Property prop: properties) {
            if(prop.key.equals(property)) {
                return prop.values;
            }
        }
        return null;
    }

    public static class Property {

        @XmlElement(name = "Key")
        private String key;

        @XmlElement(name = "Value")
        @XmlElementWrapper(name = "Values")
        private List<String> values;
    }
}
