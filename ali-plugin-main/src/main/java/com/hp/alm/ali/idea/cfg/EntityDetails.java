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

import com.hp.alm.ali.idea.entity.EntityRef;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class EntityDetails implements JDOMSerialization {

    private final List<EntityRef> refs = new ArrayList<EntityRef>();
    private EntityRef selectedRef;
    private boolean columnControls = false;

    public synchronized Element toElement(String name) {
        Element target = new Element(name);
        if(selectedRef != null) {
            target.setAttribute("selectedId", String.valueOf(selectedRef.id));
            target.setAttribute("selectedType", selectedRef.type);
        }
        target.setAttribute("controls", String.valueOf(columnControls));
        for(EntityRef ref: refs) {
            Element entity = new Element("entity");
            entity.setAttribute("id", String.valueOf(ref.id));
            entity.setAttribute("type", String.valueOf(ref.type));
            target.addContent(entity);
        }
        return target;
    }

    private EntityRef getEntityRef(Element element, String typeAttr, String idAttr) {
        String id = element.getAttributeValue(idAttr);
        String type = element.getAttributeValue(typeAttr);
        if(id != null && type != null) {
            return new EntityRef(type, Integer.valueOf(id));
        } else {
            return null;
        }
    }

    public synchronized void fromElement(Element element) {
        selectedRef = getEntityRef(element, "selectedType", "selectedId");
        refs.clear();
        for(Element child: (List<Element>)element.getChildren("entity")) {
            addRef(getEntityRef(child, "type", "id"));
        }
        columnControls = Boolean.parseBoolean(element.getAttributeValue("controls", "false"));
    }

    public synchronized List<EntityRef> getRefs() {
        return new ArrayList<EntityRef>(refs);
    }

    public synchronized void removeRef(EntityRef ref) {
        refs.remove(ref);
    }

    public synchronized void addRef(EntityRef ref) {
        if(!refs.contains(ref)) {
            refs.add(ref);
        }
    }

    public synchronized void setSelectedRef(EntityRef ref) {
        this.selectedRef = ref;
    }

    public EntityRef getSelectedRef() {
        return selectedRef;
    }

    public synchronized void setColumnControls(boolean controls) {
        this.columnControls = controls;
    }

    public synchronized boolean isColumnControls() {
        return columnControls;
    }

}
