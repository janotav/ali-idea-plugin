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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;

public class TaskConfig {

    private boolean enabled = true;
    private String storedQuery = "";
    private EntityQuery customFilter;
    private boolean customSelected = true;

    public TaskConfig() {
    }

    public TaskConfig(String entityType) {
        customFilter = new EntityQuery(entityType);
    }

    @Attribute("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Attribute("storedQuery")
    public String getStoredQuery() {
        return storedQuery;
    }

    public void setStoredQuery(String storedQuery) {
        this.storedQuery = storedQuery;
    }

    @Transient
    public EntityQuery getCustomFilter() {
        return customFilter;
    }

    public void setCustomFilter(EntityQuery customFilter) {
        this.customFilter = customFilter;
    }

    @Tag("customFilter")
    public Element getCustomFilterElement() {
        return customFilter.toElement("customFilter");
    }

    public void setCustomFilterElement(Element element) {
        customFilter = new EntityQuery(element.getName());
        customFilter.fromElement(element);
    }

    @Attribute("custom")
    public boolean isCustomSelected() {
        return customSelected;
    }

    public void setCustomSelected(boolean customSelected) {
        this.customSelected = customSelected;
    }
}
