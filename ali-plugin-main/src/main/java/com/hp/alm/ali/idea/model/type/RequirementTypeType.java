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

import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.filter.MultipleItemsChooserFactory;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.model.parser.RequirementTypeList;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.RequirementTypeService;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.List;

public class RequirementTypeType implements Type, Translator {

    private Project project;
    private RequirementTypeService requirementTypeService;

    public RequirementTypeType(Project project, RequirementTypeService requirementTypeService) {
        this.project = project;
        this.requirementTypeService = requirementTypeService;
    }

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return new MultipleItemsChooserFactory(project, "Requirement Type", true, new ItemsProvider.Loader<ComboItem>() {
            @Override
            public List<ComboItem> load() {
                RequirementTypeList types = project.getComponent(RequirementTypeService.class).getRequirementTypes();
                return FilterManager.asItems(types, "id", true, true);
            }
        });
    }

    @Override
    public FilterResolver getFilterResolver() {
        return new MultipleItemsTranslatedResolver(this);
    }

    @Override
    public Translator getTranslator() {
        return this;
    }

    @Override
    public String translate(final String value, final ValueCallback callback) {
        RequirementTypeList types = requirementTypeService.tryRequirementTypes();
        if(types == null) {
            requirementTypeService.loadRequirementTypeListAsync(new AbstractCachingService.Callback<RequirementTypeList>() {
                @Override
                public void loaded(RequirementTypeList types) {
                    callback.value(getRequirementName(types, value));
                }
            });
            return null;
        } else {
            return getRequirementName(types, value);
        }
    }

    private String getRequirementName(RequirementTypeList types, String value) {
        String ret = value;
        for(Entity type: types) {
            if(value.equals(String.valueOf(type.getId()))) {
                ret = type.getPropertyValue("name");
                break;
            }
        }
        return ret;
    }
}
