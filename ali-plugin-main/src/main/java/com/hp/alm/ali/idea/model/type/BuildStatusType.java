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
import com.hp.alm.ali.idea.translate.filter.MultipleItemsResolver;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.intellij.openapi.project.Project;

import java.util.Arrays;

public class BuildStatusType implements Type {

    private Project project;

    public BuildStatusType(Project project) {
        this.project = project;
    }

    @Override
    public FilterFactory getFilterFactory(final boolean multiple) {
        return new MultipleItemsChooserFactory(project, "Status", true, new ItemsProvider.Eager<ComboItem>(FilterManager.asItems(Arrays.asList("Success", "Warning", "Failed"), true, multiple)));
    }

    @Override
    public FilterResolver getFilterResolver() {
        return new MultipleItemsResolver();
    }

    @Override
    public Translator getTranslator() {
        return null;
    }
}
