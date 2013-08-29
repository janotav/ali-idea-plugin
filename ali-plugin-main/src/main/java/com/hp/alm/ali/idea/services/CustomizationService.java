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
import com.hp.alm.ali.idea.rest.RestListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.util.List;

public class CustomizationService implements RestListener {

    private RestService restService;
    private APMCommonSettings settings;

    public CustomizationService(RestService restService) {
        this.restService = restService;

        restService.addListener(this);
    }

    public synchronized String getNewDefectStatus(final boolean dispatch, final AbstractCachingService.Callback<String> whenLoaded) {
        if(settings == null) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    InputStream is = null;
                    try {
                        is = restService.getForStream("customization/extensions/dev/preferences");
                    } catch(Exception e) {
                        // IDE Customization extension may not be installed
                    }
                    final String value;
                    synchronized (CustomizationService.this) {
                        if(is != null) {
                            try {
                                settings = JAXBSupport.unmarshall(new BOMInputStream(is), APMCommonSettings.class);
                            } catch(Exception e) {
                                // go with defaults if anything goes wrong
                                settings = new APMCommonSettings();
                            }
                        } else {
                            settings = new APMCommonSettings();
                        }
                        value = getNewDefectStatus();
                    }
                    if(whenLoaded != null) {
                        if(dispatch) {
                            UIUtil.invokeLaterIfNeeded(new Runnable() {
                                @Override
                                public void run() {
                                    whenLoaded.loaded(value);
                                }
                            });
                        } else {
                            whenLoaded.loaded(value);
                        }
                    }
                }
            });
            return null;
        } else {
            return getNewDefectStatus();
        }
    }

    private String getNewDefectStatus() {
        List<String> list = settings.getValues("defectDefaultStatusValueForCreate");
        if(list != null && !list.isEmpty()) {
            return list.get(0);
        } else {
            return "";
        }
    }

    @Override
    public void restConfigurationChanged() {
        synchronized (this) {
            settings = null;
        }
    }
}
