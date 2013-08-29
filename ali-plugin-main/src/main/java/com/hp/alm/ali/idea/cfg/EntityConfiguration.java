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

import com.hp.alm.ali.idea.entity.EntityQuery;
import org.jdom.Element;

public class EntityConfiguration implements JDOMSerialization {

    private EntityQuery currentFilter;
    private EntityQuery lookupFilter;
    private EntityFields fields;

    public EntityConfiguration(String entityType) {
        currentFilter = new EntityQuery(entityType);
        lookupFilter = new EntityQuery(entityType);
        fields = new EntityFields();
    }

    public EntityQuery getCurrentFilter() {
        return currentFilter;
    }

    public EntityQuery getLookupFilter() {
        return lookupFilter;
    }

    public EntityFields getFields() {
        return fields;
    }

    public Element toElement(String name) {
        Element element = new Element(name);
        element.addContent(currentFilter.toElement("currentQuery"));
        element.addContent(lookupFilter.toElement("lookupQuery"));
        element.addContent(fields.toElement("details"));
        return element;
    }

    public void fromElement(Element element) {
        Element current = element.getChild("currentQuery");
        if(current != null) {
            currentFilter.fromElement(current);
        }
        Element lookup = element.getChild("lookupQuery");
        if(lookup != null) {
            lookupFilter.fromElement(lookup);
        }
        Element detailsElem = element.getChild("details");
        if(detailsElem != null) {
            fields.fromElement(detailsElem);
        }
    }
}
