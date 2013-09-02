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

import com.hp.alm.ali.idea.model.parser.JAXBSupport;
import com.hp.alm.ali.idea.cfg.APMCommonSettings;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Transform;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.util.List;

public class CustomizationService extends AbstractCachingService<Integer, APMCommonSettings, AbstractCachingService.Callback<APMCommonSettings>> {

    final private static int IDE_PREFERENCES = 1;

    private RestService restService;

    public CustomizationService(Project project) {
        super(project);

        restService = project.getComponent(RestService.class);
    }

    public String getNewDefectStatus(final AbstractCachingService.Callback<String> whenLoaded) {
        APMCommonSettings settings = getCachedValue(IDE_PREFERENCES);
        if(settings == null) {
            getValueAsync(IDE_PREFERENCES, translate(whenLoaded, new Transform<APMCommonSettings, String>() {
                @Override
                public String transform(APMCommonSettings settings) {
                    return getNewDefectStatus(settings);
                }
            }));
            return null;
        } else {
            return getNewDefectStatus(settings);
        }
    }

    private String getNewDefectStatus(APMCommonSettings settings) {
        List<String> list = settings.getValues("defectDefaultStatusValueForCreate");
        if(list != null && !list.isEmpty()) {
            return list.get(0);
        } else {
            return "";
        }
    }

    @Override
    protected APMCommonSettings doGetValue(Integer key) {
        InputStream is = null;
        try {
            is = restService.getForStream("customization/extensions/dev/preferences");
        } catch(Exception e) {
            // IDE Customization extension may not be installed
        }
        if(is != null) {
            try {
                return JAXBSupport.unmarshall(new BOMInputStream(is), APMCommonSettings.class);
            } catch(Exception e) {
                // go with defaults if anything goes wrong
            }
        }
        return new APMCommonSettings();
    }
}
