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
import com.intellij.openapi.util.Transform;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EntityLabelService extends AbstractCachingService<Integer, Map<String, String>, AbstractCachingService.Callback<Map<String, String>>> {

    // workaround the missing labels of ALI entities (<= 2.0)
    private static Map<String, String> labelMap;
    static {
        labelMap = new HashMap<String, String>();
        labelMap.put("build-type", "Build Type");
        labelMap.put("build-instance", "Build");
        labelMap.put("changeset", "Changeset");
    }

    private static final int ENTITIES = 1;

    private RestService restService;

    public EntityLabelService(Project project, RestService restService) {
        super(project);
        this.restService = restService;
    }

    public void loadEntityLabelAsync(final String entityType, Callback<String> callback) {
        getValueAsync(ENTITIES, translate(callback, new Transform<Map<String, String>, String>() {
            @Override
            public String transform(Map<String, String> labelMap) {
                if(labelMap.containsKey(entityType)) {
                    return labelMap.get(entityType);
                } else {
                    return entityType;
                }
            }
        }));
    }

    @Override
    protected Map<String, String> doGetValue(Integer key) {
        InputStream is = restService.getForStream("customization/entities");
        CustomizationList customizationList = CustomizationList.create(is);

        HashMap<String, String> labelMap = new HashMap<String, String>();
        for (String[] s : customizationList) {
            if (restService.getServerTypeIfAvailable() == ServerType.AGM && "requirement".equals(s[0])) {
                labelMap.put(s[0], "User Story"); // override value indicated in metadata
            } else {
                labelMap.put(s[0], fixMissing(s[1], s[0]));
            }
        }

        return labelMap;
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
