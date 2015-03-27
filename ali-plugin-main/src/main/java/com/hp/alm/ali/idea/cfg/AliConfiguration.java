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
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.apache.commons.codec.binary.Base64;
import org.jdom.Element;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@State(
  name = "AliConfiguration",
  storages = {
    @Storage( id = "default", file = "$APP_CONFIG$/HpAliPlugin.xml")
  }
)
public class AliConfiguration implements PersistentStateComponent<Element> {

    public static final String PROPERTY_LOCATION = "location";
    public static final String PROPERTY_DOMAIN = "domain";
    public static final String PROPERTY_PROJECT = "project";
    public static final String PROPERTY_USERNAME = "username";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_STORE_PASSWORD = "store-password";
    public static final String PROPERTY_STATUS_TRANSITION = "status-transition";
    public static final String PROPERTY_SPELL_CHECKER = "spell-checker";
    public static final String PROPERTY_DEV_MOTIVE_ANNOTATION = "dev-motive-annotation";

    public String ALM_LOCATION = "";
    public String ALM_DOMAIN = "";
    public String ALM_PROJECT = "";
    public String ALM_USERNAME = "";
    public String ALM_PASSWORD = "";
    public boolean STORE_PASSWORD = true;

    public boolean spellChecker = true;
    public boolean devMotiveAnnotation = true;

    // poor man's workflow
    public String STATUS_TRANSITION;

    private List<ConfigurationListener> listeners = new LinkedList<ConfigurationListener>();
    private Map<String, List<EntityQuery>> filters;

    public AliConfiguration() {
        filters = new HashMap<String, List<EntityQuery>>();
        STATUS_TRANSITION = Transitions.DEFAULT_STATUS_TRANSITION;
    }

    public synchronized Element getState() {
        Element element = new Element(getClass().getSimpleName());
        addProperty(element, PROPERTY_LOCATION, ALM_LOCATION);
        addProperty(element, PROPERTY_DOMAIN, ALM_DOMAIN);
        addProperty(element, PROPERTY_PROJECT, ALM_PROJECT);
        addProperty(element, PROPERTY_USERNAME, ALM_USERNAME);
        if(STORE_PASSWORD) {
            addProperty(element, PROPERTY_PASSWORD, new String(new Base64().encode(ALM_PASSWORD.getBytes())));
        }
        addProperty(element, PROPERTY_STORE_PASSWORD, STORE_PASSWORD);
        element.addContent(getStoredFilters());
        addProperty(element, PROPERTY_STATUS_TRANSITION, STATUS_TRANSITION);
        addProperty(element, PROPERTY_SPELL_CHECKER, String.valueOf(spellChecker));
        addProperty(element, PROPERTY_DEV_MOTIVE_ANNOTATION, String.valueOf(devMotiveAnnotation));
        return element;
    }

    private Element getStoredFilters() {
        Element target = new Element("stored");
        for(String entityName: filters.keySet()) {
            Element element = new Element(entityName);
            List<EntityQuery> list = filters.get(entityName);
            for(EntityQuery filter: list) {
                Element query = filter.toElement("query");
                element.addContent(query);
            }
            target.addContent(element);
        }
        return target;
    }

    public synchronized void loadState(Element element) {
        ALM_LOCATION = getProperty(element, PROPERTY_LOCATION);
        ALM_DOMAIN = getProperty(element, PROPERTY_DOMAIN);
        ALM_PROJECT = getProperty(element, PROPERTY_PROJECT);
        ALM_USERNAME = getProperty(element, PROPERTY_USERNAME);
        ALM_PASSWORD = new String(new Base64().decode(getProperty(element, PROPERTY_PASSWORD).getBytes()));
        STORE_PASSWORD = Boolean.valueOf(getProperty(element, PROPERTY_STORE_PASSWORD));
        STATUS_TRANSITION = getProperty(element, PROPERTY_STATUS_TRANSITION);
        spellChecker = Boolean.valueOf(getProperty(element, PROPERTY_SPELL_CHECKER, "true"));
        devMotiveAnnotation = Boolean.valueOf(getProperty(element, PROPERTY_DEV_MOTIVE_ANNOTATION, "true"));

        Element stored = element.getChild("stored");
        if(stored != null) {
            loadStoredFilters(stored);
        }
    }

    private void loadStoredFilters(Element element) {
        for(Element child: (List<Element>)element.getChildren()) {
            String entityName = child.getName();
            List<EntityQuery> list = new LinkedList<EntityQuery>();
            filters.put(entityName, list);
            for(Element filter: (List<Element>)child.getChildren("query")) {
                EntityQuery entityFilter = new EntityQuery(entityName);
                entityFilter.fromElement(filter);
                list.add(entityFilter);
            }
        }
    }

    public synchronized List<EntityQuery> getStoredFilters(String entityName) {
        List<EntityQuery> queries = filters.get(entityName);
        if(queries == null) {
            queries = new LinkedList<EntityQuery>();
            filters.put(entityName, queries);
        }
        return new LinkedList<EntityQuery>(queries);
    }

    public synchronized void storeFilter(String entityType, EntityQuery filter) {
        dropFilter(entityType, filter.getName());
        filters.get(entityType).add(filter);
    }

    public synchronized void dropFilter(String entityType, String filterName) {
        // make sure that filters are initialized for this type
        getStoredFilters(entityType);

        List<EntityQuery> stored = filters.get(entityType);
        for(EntityQuery f: stored) {
            if(f.getName().equals(filterName)) {
                stored.remove(f);
                return;
            }
        }
    }

    public String getLocation() {
        return ALM_LOCATION;
    }

    public String getDomain() {
        return ALM_DOMAIN;
    }

    public String getProject() {
        return ALM_PROJECT;
    }

    public String getUsername() {
        return ALM_USERNAME;
    }

    public String getPassword() {
        return ALM_PASSWORD;
    }

    protected String getProperty(Element element, String name, String defaultValue) {
        Element child = element.getChild(name);
        if(child != null) {
            return child.getText();
        } else {
            return defaultValue;
        }
    }

    protected String getProperty(Element element, String name) {
        return getProperty(element, name, "");
    }

    protected void addProperty(Element element, String name, Object value) {
        Element property = new Element(name);
        property.setText(value.toString());
        element.addContent(property);
    }

    public void addListener(ConfigurationListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConfigurationListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    public void fireChanged() {
        synchronized(listeners) {
            for(ConfigurationListener listener: listeners) {
                listener.onChanged();
            }
        }
    }
}
