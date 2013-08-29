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

import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.intellij.openapi.project.Project;

import java.util.List;

public class MultipleItemsFactory implements FilterFactory {

    private Project project;
    private final String title;
    private final boolean multiple;
    private final ItemsProvider<ComboItem> items;
    private Translator translator;

    public MultipleItemsFactory(Project project, String title, boolean multiple, ItemsProvider<ComboItem> items) {
        this(project, title, multiple, items, null);
    }

    public MultipleItemsFactory(Project project, String title, boolean multiple, ItemsProvider<ComboItem> items, Translator translator) {
        this.project = project;
        this.title = title;
        this.multiple = multiple;
        this.items = items;
        this.translator = translator;
    }

    @Override
    public FilterChooser createChooser(String value) {
        return new MultipleItemsChooser(project, title, multiple, items, translator, value);
    }

    @Override
    public List<String> getCustomChoices() {
        return null;
    }
}
