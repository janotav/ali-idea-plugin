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

import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.model.Entity;

public class DefectLinkTypeType implements Type {

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterResolver getFilterResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Translator getTranslator() {
        return new TypeTranslator();
    }

    private class TypeTranslator implements Translator, ContextAware {

        private Context context;

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
                return entity.getPropertyValue("second-endpoint-type");
            } else {
                return "defect";
            }
        }
    }
}
