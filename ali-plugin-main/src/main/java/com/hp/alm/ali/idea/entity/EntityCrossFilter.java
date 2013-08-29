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

package com.hp.alm.ali.idea.entity;

import org.jdom.Element;

final public class EntityCrossFilter extends AbstractEntityFilter<EntityCrossFilter> {

    private boolean inclusive;

    public EntityCrossFilter(String entityType) {
        super(entityType);
        this.inclusive = true;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public Element toElement(String name) {
        Element filter = super.toElement(name);
        if(!isInclusive()) {
            filter.setAttribute("inclusive", "false");
        }
        return filter;
    }

    public void fromElement(Element element) {
        super.fromElement(element);
        setInclusive(Boolean.valueOf(element.getAttributeValue("inclusive", "true")));
    }

    public synchronized EntityCrossFilter clone() {
        EntityCrossFilter cf = new EntityCrossFilter(entityType);
        cf.copyFrom(this);
        return cf;
    }

    public synchronized void copyFrom(EntityCrossFilter other) {
        synchronized (other) {
            super.copyFrom(other);
            setInclusive(other.isInclusive());
        }
    }
}
