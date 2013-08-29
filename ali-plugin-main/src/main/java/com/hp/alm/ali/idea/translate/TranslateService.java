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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.entity.SimpleCache;
import com.hp.alm.ali.idea.model.type.ContextAware;
import com.hp.alm.ali.idea.model.type.Type;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.translate.filter.ExpressionResolver;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

public class TranslateService implements ServerTypeListener {

    public static final String LOADING_MESSAGE = "Loading...";
    public static final String NA_TOKEN = "N/A";

    private Project project;
    private SimpleCache cache;

    public TranslateService(Project project, RestService restService) {
        this.project = project;

        cache = new SimpleCache();

        restService.addServerTypeListener(this);
    }

    public boolean isTranslated(Field field) {
        return getTranslator(field) != null;
    }

    public String convertQueryModelToREST(Field field, String value) {
        FilterResolver resolver = getFilterResolver(field);
        if(resolver != null) {
            return resolver.toRESTQuery(value);
        } else {
            return value;
        }
    }

    public String convertQueryModelToView(Field field, String value, ValueCallback callback) {
        FilterResolver resolver = getFilterResolver(field);
        if(resolver != null) {
            return resolver.resolveDisplayValue(value, callback);
        } else {
            callback.value(value);
            return value;
        }
    }

    public FilterResolver getFilterResolver(Field field) {
        if(Type.class.isAssignableFrom(field.getClazz())) {
            Type type = (Type)project.getComponent(field.getClazz());
            FilterResolver filterResolver = type.getFilterResolver();
            if(filterResolver != null) {
                return filterResolver;
            }
        }
        if(field.getReferencedType() != null) {
            return new ExpressionResolver(getReferenceTranslator(field.getReferencedType()));
        }
        if(field.getListId() != null) {
            return new MultipleItemsResolver();
        }
        return null;
    }

    public Translator getTranslator(Field field) {
        if(Type.class.isAssignableFrom(field.getClazz())) {
            Type type = (Type)project.getComponent(field.getClazz());
            Translator translator = type.getTranslator();
            if(translator != null) {
                return translator;
            }
        }
        if(field.getReferencedType() != null) {
            return getReferenceTranslator(field.getReferencedType());
        }
        return null;
    }

    public Translator getReferenceTranslator(String targetType) {
        return new EntityReferenceTranslator(project, targetType, cache);
    }

    public String translateAsync(Field field, final String value, boolean syncCall, final ValueCallback onValue) {
        return translateAsync(getTranslator(field), value, syncCall, onValue);
    }

    public String translateAsync(Translator translator, final String value, boolean syncCall, final ValueCallback onValue) {
        if((value == null || value.isEmpty()) && !(translator instanceof ContextAware)) {
            if(syncCall) {
                onValue.value(value);
            }
            return value;
        }
        Proxy proxy = new Proxy(onValue);
        synchronized (proxy) {
            String result = translator.translate(value, proxy);
            if(result != null) {
                if(syncCall) {
                    onValue.value(result);
                }
                return result;
            }
            return LOADING_MESSAGE;
        }
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            cache.clear();
        }
    }

    private static class Proxy implements ValueCallback {

        private ValueCallback callback;

        public Proxy(ValueCallback callback) {
            this.callback = callback;
        }

        @Override
        public synchronized void value(final String value) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    callback.value(value);
                }
            });
        }
    }

}
