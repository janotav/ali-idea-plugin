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

import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.model.User;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.ProjectUserService;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.filter.MultipleItemsFactory;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class UserType implements Type, Translator, FilterResolver {

    private Project project;
    private ProjectUserService projectUserService;

    public UserType(Project project, ProjectUserService projectUserService) {
        this.project = project;
        this.projectUserService = projectUserService;
    }

    @Override
    public FilterFactory getFilterFactory(final boolean multiple) {
        return new MultipleItemsFactory(project, "User", multiple, new ItemsProvider.Loader<ComboItem>() {
            @Override
            public List<ComboItem> load() {
                return asItemsUsers(projectUserService.getUserList(), multiple, false);
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
    public String translate(String value, final ValueCallback onValue) {
        User user = projectUserService.tryGetUser(value);
        if(user == null) {
            projectUserService.loadUserAsync(value, new AbstractCachingService.Callback<User>() {
                @Override
                public void loaded(User user) {
                    onValue.value(userName(user, false));
                }
            });
            return null;
        } else {
            return userName(user, false);
        }
    }

    private String userName(User user, boolean longName) {
        String fullName = user.getFullName();
        if(fullName.isEmpty()) {
            return user.getUsername();
        } else if(longName) {
            return fullName + " (" + user.getUsername() + ")";
        } else {
            return fullName;
        }
    }

    @Override
    public String resolveDisplayValue(String value, final ValueCallback onValue) {
        onValue.value(value);
        projectUserService.loadUserAsync(value, new AbstractCachingService.Callback<User>() {
            @Override
            public void loaded(User user) {
                onValue.value(user.getFullName());
            }
        });
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        return value;
    }

    private List<ComboItem> asItemsUsers(List<User> list, boolean multiple, boolean required) {
        ArrayList<ComboItem> items = new ArrayList<ComboItem>();
        for(User item: list) {
            items.add(new ComboItem(item.getUsername(), userName(item, true)));
        }
        if(!required) {
            items.add(new ComboItem(multiple? MultipleItemsTranslatedResolver.NO_VALUE: "", MultipleItemsTranslatedResolver.NO_VALUE_DESC));
        }
        return items;
    }
}
