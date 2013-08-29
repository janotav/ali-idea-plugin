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

package com.hp.alm.ali.idea.translate;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.SimpleCache;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityReferenceTranslator implements Translator {

    private static Map<String, List<String>> map;
    static {
        map = new HashMap<String, List<String>>();
        map.put("build-instance", Arrays.asList("{1} #{0}", "number", "type.name"));
        map.put("changeset", Arrays.asList("{0} {1}: {2}", "date", "owner", "description"));
    }

    private SimpleCache cache;
    private String targetType;
    private String mask;
    private List<String> fields;
    private EntityService entityService;
    private MetadataService metadataService;

    public EntityReferenceTranslator(Project project, String targetType, SimpleCache cache) {
        this.cache = cache;
        this.targetType = targetType;
        entityService = project.getComponent(EntityService.class);
        metadataService = project.getComponent(MetadataService.class);

        List<String> maskAndFields = map.get(targetType);
        if(maskAndFields == null) {
            mask = "{0}";
            fields = Arrays.asList("name");
        } else {
            mask = maskAndFields.get(0);
            fields = maskAndFields.subList(1, maskAndFields.size());
        }
    }

    @Override
    public String translate(String value, ValueCallback callback) {
        return new Execution(callback).perform(value);
    }

    private class Execution implements EntityListener {

        private Entity targetEntity;
        private EntityRef targetRef;
        private Set<EntityRef> requested;
        private ValueCallback callback;

        private Execution(ValueCallback callback) {
            this.callback = callback;

            requested = new HashSet<EntityRef>();
        }

        private String perform(String value) {
            final Set<String> props = new HashSet<String>();
            for(String field: fields) {
                if(field.contains(".")) {
                    props.add(field.substring(0, field.indexOf(".")));
                } else {
                    props.add(field);
                }
            }

            targetRef = new EntityRef(targetType, Integer.valueOf(value));
            targetEntity = cache.lookup(targetRef, props);
            if(targetEntity != null) {
                return calculateValue();
            } else {
                if(cache.isNotFound(targetRef)) {
                    return TranslateService.NA_TOKEN;
                }
                requested.add(targetRef);
                entityService.requestCachedEntity(targetRef, new LinkedList<String>(props), this);
                return null;
            }
        }

        @Override
        public void entityLoaded(Entity entity, Event event) {
            cache.add(entity);

            if(new EntityRef(entity).equals(targetRef)) {
                targetEntity = entity;
            }

            setValue(calculateValue());
        }

        @Override
        public void entityNotFound(EntityRef ref, boolean removed) {
            cache.addNotFound(ref);

            if(ref.equals(targetRef)) {
                setValue(TranslateService.NA_TOKEN);
            } else {
                setValue(calculateValue());
            }
        }

        private String resolveField(SimpleCache cache, Entity entity, List<String> field) {
            String value = entity.getPropertyValue(field.get(0));
            if(value.isEmpty() || field.size() == 1) {
                return value;
            } else {
                int targetId = Integer.valueOf(value);
                String targetType = getFieldTarget(entity, field.get(0));
                EntityRef targetRef = new EntityRef(targetType, targetId);
                List<String> props = Arrays.asList(field.get(1));
                Entity targetEntity = cache.lookup(targetRef, props);
                if(targetEntity != null) {
                    return resolveField(cache, targetEntity, field.subList(1, field.size()));
                } else if(cache.isNotFound(targetRef)) {
                    return TranslateService.NA_TOKEN;
                } else if(requested.add(targetRef)) {
                    entityService.requestCachedEntity(targetRef, props, this);
                }
                return null;
            }
        }

        private String getFieldTarget(Entity entity, String fieldName) {
            Field field = metadataService.getEntityMetadata(entity.getType()).getField(fieldName);
            return field.resolveReference(entity);
        }

        private String calculateValue() {
            List<String> values = new LinkedList<String>();
            for(String field: fields) {
                String fieldValue = resolveField(cache, targetEntity, Arrays.asList(field.split("\\.")));
                if(fieldValue == null) {
                    return null;
                }
                values.add(fieldValue);
            }
            return MessageFormat.format(mask, values.toArray());
        }

        private void setValue(final String value) {
            if(value != null) {
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        callback.value(value);
                    }
                });
            }
        }
    }
}
