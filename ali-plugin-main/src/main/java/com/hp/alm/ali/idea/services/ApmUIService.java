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

import com.google.gson.Gson;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.model.AuthenticationInfo;
import com.hp.alm.ali.idea.rest.MyInputData;
import com.hp.alm.ali.idea.rest.MyResultInfo;
import com.hp.alm.ali.idea.rest.RestException;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;

import java.util.Collections;

public class ApmUIService {

    private RestService restService;
    private EntityService entityService;
    private ErrorService errorService;

    public ApmUIService(RestService restService, EntityService entityService, ErrorService errorService) {
        this.restService = restService;
        this.entityService = entityService;
        this.errorService = errorService;
    }

    public Entity createDefectInRelease(String description, String summary, String severity, String detectedBy,
                                        int releaseId, int sprintId, int teamId, int featureId, int workspaceId) {
        StringBuffer buf = new StringBuffer();
        buf.append("description=").append(EntityQuery.encode(description));
        buf.append("&detectedBy=").append(EntityQuery.encode(detectedBy));
        buf.append("&detectedOn=");
        buf.append("&featureID=").append(featureId > 0? featureId: 0);
        buf.append("&name=").append(EntityQuery.encode(summary));
        buf.append("&releaseId=").append(releaseId);
        buf.append("&sevirity=").append(EntityQuery.encode(severity));
        buf.append("&sprintID=").append(sprintId);
        buf.append("&teamID=").append(teamId);
        buf.append("&productGroupId=").append(workspaceId);

        return createItem("apmuiservices/additemservice/createdefectinrelease", buf.toString());
    }

    public Entity createRequirementInRelease(String description, String name, String priority, int storyPoints,
                                             int releaseId, int sprintId, int teamId, int featureId, int workspaceId) {
        StringBuffer buf = new StringBuffer();
        buf.append("description=").append(EntityQuery.encode(description));
        buf.append("&featureID=").append(featureId);
        buf.append("&name=").append(EntityQuery.encode(name));
        buf.append("&parentID=0");
        buf.append("&priority=").append(EntityQuery.encode(priority));
        buf.append("&releaseId=").append(releaseId);
        buf.append("&reqType=70");
        buf.append("&sprintID=").append(sprintId);
        buf.append("&storyPoints=").append(storyPoints);
        buf.append("&teamID=").append(teamId);
        buf.append("&productGroupId=").append(workspaceId);

        return createItem("apmuiservices/additemservice/createrequirementinrelease", buf.toString());
    }

    public AuthenticationInfo getAuthenticationInfo() {
        MyResultInfo result = new MyResultInfo();
        int code = restService.get(result, "apmuiservices/configurationusers/authentication-info");
        if(code == 200) {
            return new Gson().fromJson(result.getBodyAsString(), AuthenticationInfo.class);
        } else {
            errorService.showException(new RestException(result));
            return null;
        }
    }

    private Entity createItem(String href, String value) {
        MyResultInfo result = new MyResultInfo();
        MyInputData data = new MyInputData(value, Collections.singletonMap("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"));
        int code = restService.post(data, result, href);
        if(code == 200) {
            EntityList list = EntityList.create(result.getBodyAsStream());
            Entity backlogItem = list.get(0);
            Entity entity = list.get(1);
            entity.mergeRelatedEntity(backlogItem);
            entityService.fireEntityLoaded(backlogItem, EntityListener.Event.CREATE);
            entityService.fireEntityLoaded(entity, EntityListener.Event.CREATE);
            // if tasks are created implicitly, fire create events for them too
            if(!"0".equals(backlogItem.getPropertyValue("no-of-sons"))) {
                EntityQuery query = new EntityQuery("project-task");
                query.setValue("release-backlog-item-id", String.valueOf(backlogItem.getId()));
                EntityList tasks = entityService.query(query);
                for(Entity task: tasks) {
                    entityService.fireEntityLoaded(task, EntityListener.Event.CREATE);
                }
            }
            return entity;
        } else {
            errorService.showException(new RestException(result));
            return null;
        }
    }
}
