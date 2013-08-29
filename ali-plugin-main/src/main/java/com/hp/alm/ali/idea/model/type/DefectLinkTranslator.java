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

package com.hp.alm.ali.idea.model.type;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.Collections;

class DefectLinkTranslator implements Translator, ContextAware {

    private Context context;
    private String property;
    private EntityService entityService;

    public DefectLinkTranslator(Project project, String property) {
        entityService = project.getComponent(EntityService.class);
        this.property = property;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String translate(String value, final ValueCallback callback) {
        if(context == null) {
            throw new IllegalStateException("entity context not initialized");
        }

        Entity master = context.getMasterEntity();
        Entity entity = context.getEntity();
        if("defect".equals(master.getType()) && entity.getPropertyValue("first-endpoint-id").equals(master.getPropertyValue("id"))) {
            return entity.getPropertyValue("second-endpoint-" + property);
        }
        value = context.get(property);
        if(value == null) {
            EntityRef ref = new EntityRef("defect", Integer.valueOf(entity.getPropertyValue("first-endpoint-id")));
            entityService.requestCachedEntity(ref, Collections.singletonList(property), new EntityListener() {
                @Override
                public void entityLoaded(Entity entity, Event event) {
                    String value = entity.getPropertyValue(property);
                    context.put(property, value);
                    callback.value(value);
                }

                @Override
                public void entityNotFound(EntityRef ref, boolean removed) {
                    context.put(property, "N/A");
                    callback.value("N/A");
                }
            });
        }
        return value;
    }
}
