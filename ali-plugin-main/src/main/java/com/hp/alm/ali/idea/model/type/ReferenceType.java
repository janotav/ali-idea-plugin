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

import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.intellij.openapi.project.Project;

public abstract class ReferenceType implements Type {

    protected Project project;
    protected TranslateService translateService;
    private String targetType;

    public ReferenceType(Project project, String targetType) {
        this.project = project;
        this.targetType = targetType;
        translateService = project.getComponent(TranslateService.class);
    }

    @Override
    public FilterResolver getFilterResolver() {
        return new MultipleItemsTranslatedResolver(translateService.getReferenceTranslator(targetType));
    }

    @Override
    public Translator getTranslator() {
        return null;
    }
}
