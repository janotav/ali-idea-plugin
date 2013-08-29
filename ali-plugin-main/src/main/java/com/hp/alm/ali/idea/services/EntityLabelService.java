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

import com.hp.alm.ali.idea.model.parser.CustomizationList;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.intellij.openapi.project.Project;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EntityLabelService extends AbstractCachingService<String, String, AbstractCachingService.Callback<String>> {

    // workaround the missing labels of ALI entities (<= 2.0)
    private static Map<String, String> labelMap;
    static {
        labelMap = new HashMap<String, String>();
        labelMap.put("build-type", "Build Type");
        labelMap.put("build-instance", "Build");
        labelMap.put("changeset", "Changeset");
    }

    private RestService restService;
    private CustomizationList customizationList;

    public EntityLabelService(Project project, RestService restService) {
        super(project);
        this.restService = restService;
    }

    public void loadEntityLabelAsync(String entityType, Callback<String> callback) {
        getValueAsync(entityType, callback);
    }

    public String getCachedEntityLabel(String entityType, Callback<String> callback) {
        String cachedValue = getCachedValue(entityType);
        if(cachedValue != null) {
            return cachedValue;
        } else {
            if(callback != null) {
                loadEntityLabelAsync(entityType, callback);
            }
            return entityType;
        }
    }

    @Override
    protected String doGetValue(String entityType) {
        if (restService.getServerTypeIfAvailable() == ServerType.AGM && "requirement".equals(entityType)) {
            return "User Story"; // override value indicated in metadata
        }

        synchronized (this) {
            if(customizationList == null) {
                InputStream is = restService.getForStream("customization/entities");
                customizationList = CustomizationList.create(is);
            }
            for (String[] s : customizationList) {
                if(s[0].equals(entityType)) {
                    return fixMissing(s[1], entityType);
                }
            }

            return entityType;
        }
    }

    private String fixMissing(String value, String entityType) {
        if(value.equals("No user label defined for "+entityType)) {
            if(labelMap.containsKey(entityType)) {
                return labelMap.get(entityType);
            } else {
                return entityType;
            }
        } else {
            return value;
        }
    }
}
