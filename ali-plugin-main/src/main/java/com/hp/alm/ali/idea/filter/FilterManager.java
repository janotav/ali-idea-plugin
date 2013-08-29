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

package com.hp.alm.ali.idea.filter;

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.model.type.ContextAware;
import com.hp.alm.ali.idea.model.type.Type;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class FilterManager {

    private Project project;
    private ProjectListService projectListService;
    private TranslateService translateService;

    public FilterManager(Project project, ProjectListService projectListService, TranslateService translateService) {
        this.project = project;
        this.projectListService = projectListService;
        this.translateService = translateService;
    }

    public FilterFactory getFilterFactory(Context context, final String entityType, final Field field, final boolean multiple) {
        if(Type.class.isAssignableFrom(field.getClazz())) {
            Type type = (Type)project.getComponent(field.getClazz());
            FilterFactory filterFactory = type.getFilterFactory(multiple);
            if(filterFactory instanceof ContextAware) {
                ((ContextAware) filterFactory).setContext(context);
            }
            return filterFactory;
        }
        if(field.getListId() != null && multiple) {
            return new MultipleItemsFactory(project, field.getLabel(), true, new ItemsProvider.Loader<ComboItem>() {
                @Override
                public List<ComboItem> load() {
                    List<String> list = projectListService.getProjectList(entityType, field);
                    return asItems(list, multiple, field.isRequired());
                }
            });
        }

        if(field.getReferencedType() != null) {
            return new FilterFactoryImpl(project, field.getReferencedType(), multiple, translateService.isTranslated(field));
        } else {
            return null;
        }
    }


    public static List<ComboItem> asItems(List<String> list, boolean multiple, boolean required) {
        ArrayList<ComboItem> items = new ArrayList<ComboItem>();
        for(String item: list) {
            items.add(new ComboItem(item));
        }
        if(!required) {
            items.add(new ComboItem(multiple? MultipleItemsTranslatedResolver.NO_VALUE: "", MultipleItemsTranslatedResolver.NO_VALUE_DESC));
        }
        return items;
    }

    public static List<ComboItem> asItems(List<Entity> list, String keyProperty, boolean multiple, boolean required) {
        return asItems(list, keyProperty, "name", multiple, required);
    }

    public static List<ComboItem> asItems(List<Entity> list, String keyProperty, String valueProperty, boolean multiple, boolean required) {
        ArrayList<ComboItem> items = new ArrayList<ComboItem>();
        for(Entity item: list) {
            items.add(new ComboItem(item.getPropertyValue(keyProperty), item.getPropertyValue(valueProperty)));
        }
        if(!required) {
            items.add(new ComboItem(multiple? MultipleItemsTranslatedResolver.NO_VALUE: "", MultipleItemsTranslatedResolver.NO_VALUE_DESC));
        }
        return items;
    }
}
