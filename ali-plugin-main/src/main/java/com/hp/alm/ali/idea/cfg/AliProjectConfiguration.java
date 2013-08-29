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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@State(
  name = "AliProjectConfiguration",
  storages = { @Storage(id = "default",file = "$WORKSPACE_FILE$") }
)
public class AliProjectConfiguration extends AliConfiguration {

    public static final int COMMENTS_HISTORY_LIMIT = 50;

    public Map<String, EntityConfiguration> CONF;
    public EntityDetails details;
    public Collection<String> comments;
    private String selectedContent;

    public AliProjectConfiguration() {
        CONF = new HashMap<String, EntityConfiguration>();

        details = new EntityDetails();
        comments = new LinkedList<String>();
        STATUS_TRANSITION = "";
    }

    public Element getState() {
        Element element = super.getState();
        Element entity = new Element("entity");
        for(String entityType: CONF.keySet()) {
            storeEntityConfiguration(entity, entityType);
        }
        element.addContent(entity);
        element.addContent(XmlSerializer.serialize(new CommentHistory(comments)));
        element.addContent(details.toElement("details"));
        if(selectedContent != null) {
            element.setAttribute("selectedContent", selectedContent);
        }
        return element;
    }

    public void loadState(Element element) {
        super.loadState(element);
        selectedContent = element.getAttributeValue("selectedContent");
        Element entity = element.getChild("entity");
        if(entity != null) {
            for(Element child: (List<Element>)entity.getChildren()) {
                loadEntityConfiguration(child, child.getName());
            }
        }
        Element ch = element.getChild(CommentHistory.class.getSimpleName());
        if(ch != null) {
            comments = XmlSerializer.deserialize(ch, CommentHistory.class).getComments();
        }
        Element detailsElement = element.getChild("details");
        if(detailsElement != null) {
            details.fromElement(detailsElement);
        }
    }

    public EntityDetails getDetails() {
        return details;
    }

    private void loadEntityConfiguration(Element element, String entityType) {
        EntityConfiguration configuration = new EntityConfiguration(entityType);
        configuration.fromElement(element);
        CONF.put(entityType, configuration);
    }

    private void storeEntityConfiguration(Element element, String entityType) {
        element.addContent(CONF.get(entityType).toElement(entityType));
    }


    public EntityQuery getFilter(String entityName) {
        return getOrCreate(entityName).getCurrentFilter();
    }

    public EntityQuery getLookupFilter(String entityName) {
        return getOrCreate(entityName).getLookupFilter();
    }

    public EntityFields getFields(String entityName) {
        return getOrCreate(entityName).getFields();
    }

    private EntityConfiguration getOrCreate(String entityName) {
        EntityConfiguration configuration = CONF.get(entityName);
        if(configuration == null) {
            configuration = new EntityConfiguration(entityName);
            CONF.put(entityName, configuration);
        }
        return configuration;
    }

    public String getLocation() {
        if(ALM_LOCATION.isEmpty()) {
            return ApplicationManager.getApplication().getComponent(AliConfiguration.class).ALM_LOCATION;
        } else {
            return ALM_LOCATION;
        }
    }

    public String getDomain() {
        if(ALM_DOMAIN.isEmpty()) {
            return ApplicationManager.getApplication().getComponent(AliConfiguration.class).ALM_DOMAIN;
        } else {
            return ALM_DOMAIN;
        }
    }

    public String getProject() {
        if(ALM_PROJECT.isEmpty()) {
            return ApplicationManager.getApplication().getComponent(AliConfiguration.class).ALM_PROJECT;
        } else {
            return ALM_PROJECT;
        }
    }

    public String getUsername() {
        if(ALM_USERNAME.isEmpty()) {
            return ApplicationManager.getApplication().getComponent(AliConfiguration.class).ALM_USERNAME;
        } else {
            return ALM_USERNAME;
        }
    }

    public String getPassword() {
        if(ALM_PASSWORD.isEmpty()) {
            return ApplicationManager.getApplication().getComponent(AliConfiguration.class).ALM_PASSWORD;
        } else {
            return ALM_PASSWORD;
        }
    }

    public boolean addComment(String comment) {
        if(!comments.contains(comment)) {
            if(comments.size() >= COMMENTS_HISTORY_LIMIT) {
                comments.remove(0);
            }
            return comments.add(comment);
        } else {
            return false;
        }
    }

    public void removeComment(String comment) {
        comments.remove(comment);
    }

    public List<String> getComments() {
        return new LinkedList<String>(comments);
    }

    public Transitions getStatusTransitions() {
        if(STATUS_TRANSITION.isEmpty()) {
            return new Transitions(ApplicationManager.getApplication().getComponent(AliConfiguration.class).STATUS_TRANSITION);
        } else {
            return new Transitions(STATUS_TRANSITION);
        }
    }

    public void fireColumnsChanged() {
        for(EntityConfiguration conf: CONF.values()) {
            conf.getFields().fireColumnsChanged(null);
        }
    }

    public void setSelectedContent(String selectedContent) {
        this.selectedContent = selectedContent;
    }

    public String getSelectedContent() {
        return selectedContent;
    }
}
