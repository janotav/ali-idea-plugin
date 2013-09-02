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

package com.hp.alm.ali.idea;

import com.hp.alm.ali.Handler;
import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.intellij.openapi.project.Project;

public class RestInvocations {

    public static void sprintService_getReleases(Handler handler) {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/releases?fields=id,name,start-date,end-date&query={}&order-by={}", 200)
                .content("no_entities.xml");
    }

    public static void loadMetadata(Handler handler, String entityType) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/" + entityType + "/fields", 200)
                .content("customization_" + shortName(entityType, handler.getVersion()) + "_fields.xml");

        switch (handler.getVersion()) {
            case AGM:
            case ALM12:
                handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/" + entityType + "/relations", 200)
                        .content("customization_" + shortName(entityType, handler.getVersion()) + "_relations.xml");
        }
    }

    public static void loadProjectLists(Handler handler, String entityType) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/" + entityType + "/lists", 200)
                .content("customization_" + shortName(entityType, handler.getVersion()) + "_lists.xml");
    }

    public static void loadProjectUsers(Handler handler) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/users", 200)
                .content("customization_users.xml");
    }

    public static void loadRequirementTypes(Handler handler) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/requirement/types", 200)
                .content("customization_requirement_types.xml");
    }

    public static void loadCustomizationEntities(Handler handler, Project project) {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities", 200)
                .content("customization_entities.xml");
        handler.addCleanup(new CacheCleanup(project.getComponent(EntityLabelService.class)));
    }

    private static String shortName(String entityType, ServerVersion version) {
        if("release-backlog-item".equals(entityType)) {
            return "rbi";
        } else if("release-cycle".equals(entityType) && version == ServerVersion.AGM) {
            return "sprint";
        } else if("project-task".equals(entityType)) {
            return "task";
        } else if("defect-link".equals(entityType)) {
            return "dlink";
        } else if("build-instance".equals(entityType)) {
            return "build";
        } else if("build-type".equals(entityType)) {
            return "btype";
        } else {
            return entityType;
        }
    }

    private static class CacheCleanup<E extends AbstractCachingService> implements Runnable {

        private E service;

        public CacheCleanup(E service) {
            this.service = service;
        }

        @Override
        public void run() {
            service.connectedTo(ServerType.NONE);
        }
    }
}
