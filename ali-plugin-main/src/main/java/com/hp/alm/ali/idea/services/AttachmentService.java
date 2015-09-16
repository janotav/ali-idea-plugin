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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.rest.client.XMLOutputterFactory;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.progress.IndicatingInputStream;
import com.hp.alm.ali.idea.rest.MyInputData;
import com.hp.alm.ali.idea.rest.MyResultInfo;
import com.hp.alm.ali.idea.rest.RestException;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import org.apache.commons.httpclient.HttpStatus;
import org.jdom.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AttachmentService {

    private RestService restService;
    private EntityService entityService;
    private ErrorService errorService;

    public AttachmentService(RestService restService, EntityService entityService, ErrorService errorService) {
        this.restService = restService;
        this.entityService = entityService;
        this.errorService = errorService;
    }

    public Entity getAttachmentEntity(Entity entity) {
        if(!"attachment".equals(entity.getType())) {
            throw new IllegalArgumentException("not an attachment entity");
        }
        return getAttachmentEntity(entity.getPropertyValue("name"), new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id"))));
    }

    public Entity getAttachmentEntity(String name, EntityRef parent) {
        MyResultInfo result = new MyResultInfo();
        int ret = restService.get(result, "{0}s/{1}/attachments/{2}?alt={3}", parent.type, parent.id, EntityQuery.encode(name), EntityQuery.encode("application/xml"));
        if(ret != HttpStatus.SC_OK) {
            errorService.showException(new RestException(result));
            return null;
        }
        EntityList list = EntityList.create(result.getBodyAsStream(), true);
        return list.get(0);
    }

    public String createAttachment(String filename, IndicatingInputStream is, long length, EntityRef parent) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("Slug", filename);
        MyResultInfo result = new MyResultInfo();
        if(restService.post(new MyInputData(is, length, headers), result, "{0}s/{1}/attachments", parent.type, parent.id) != HttpStatus.SC_CREATED) {
            errorService.showException(new RestException(result.getBodyAsString(), result.getLocation()));
            return null;
        } else {
            EntityList list = EntityList.create(result.getBodyAsStream(), true);
            if(!list.isEmpty()) {
                Entity entity = list.get(0);
                entityService.fireEntityLoaded(entity, EntityListener.Event.CREATE);
                return entity.getPropertyValue("name");
            } else {
                return null;
            }
        }
    }

    public boolean updateAttachmentContent(String name, EntityRef parent, IndicatingInputStream is, long length, boolean silent) {
        Map<String,String> headers = Collections.singletonMap("Content-Type", "application/octet-stream");
        MyResultInfo result = new MyResultInfo();
        if(restService.put(new MyInputData(is, length, headers), result, "{0}s/{1}/attachments/{2}", parent.type, parent.id, EntityQuery.encode(name)) != HttpStatus.SC_OK) {
            if(!silent) {
                errorService.showException(new RestException(result));
            }
            return false;
        } else {
            EntityList list = EntityList.create(result.getBodyAsStream(), true);
            if(!list.isEmpty()) {
                entityService.fireEntityLoaded(list.get(0), EntityListener.Event.GET);
            }
            return true;
        }
    }

    public Entity updateAttachmentProperty(String name, EntityRef parent, String propertyName, String propertyValue, boolean silent) {
        Entity attachment = new Entity("attachment", 0);
        attachment.setProperty(propertyName, propertyValue);
        String xml = XMLOutputterFactory.getXMLOutputter().outputString(new Document(attachment.toElement(Collections.singleton(propertyName))));
        MyResultInfo result = new MyResultInfo();
        if(restService.put(xml, result, "{0}s/{1}/attachments/{2}", parent.type, parent.id, EntityQuery.encode(name)) != HttpStatus.SC_OK) {
            if(!silent) {
                errorService.showException(new RestException(result));
            }
            return null;
        }
        EntityList list = EntityList.create(result.getBodyAsStream(), true);
        if(!list.isEmpty()) {
            Entity entity = list.get(0);
            entityService.fireEntityLoaded(entity, EntityListener.Event.GET);
            return entity;
        } else {
            return null;
        }
    }

    public void deleteAttachment(String name, EntityRef parent) {
        MyResultInfo result = new MyResultInfo();
        if(restService.delete(result, "{0}s/{1}/attachments/{2}", parent.type, parent.id, EntityQuery.encode(name)) != HttpStatus.SC_OK) {
            errorService.showException(new RestException(result));
        } else {
            EntityList list = EntityList.create(result.getBodyAsStream());
            if(!list.isEmpty()) {
                entityService.fireEntityNotFound(new EntityRef(list.get(0)), true);
            }
        }
    }
}
