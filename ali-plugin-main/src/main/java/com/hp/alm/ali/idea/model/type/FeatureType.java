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
import com.hp.alm.ali.idea.services.ThemeFeatureService;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import java.util.List;

public class FeatureType extends ReferenceType {

    private ThemeFeatureService themeFeatureService;

    public FeatureType(Project project) {
        super(project, "requirement");
        themeFeatureService = project.getComponent(ThemeFeatureService.class);
    }

    @Override
    public FilterFactory getFilterFactory(final boolean multiple) {
        return new MultipleItemsChooserFactory(project, "Feature", multiple, new ItemsProvider<ComboItem>() {
            @Override
            public boolean load(String filter, List<ComboItem> items) {
                EntityList features = themeFeatureService.getFeatures(filter);
                items.addAll(FilterManager.asItems(features, "id", multiple, false));
                return features.getTotal() > features.size();
            }
        }, translateService.getReferenceTranslator("requirement"));
    }
}
