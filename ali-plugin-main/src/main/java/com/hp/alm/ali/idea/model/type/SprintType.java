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

import com.hp.alm.ali.idea.entity.EntityFilter;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.filter.MultipleItemsFactory;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SprintType extends ReferenceType {

    private SprintService sprintService;
    private EntityService entityService;

    public SprintType(Project project, SprintService sprintService, EntityService entityService) {
        super(project, "release-cycle");
        this.sprintService = sprintService;
        this.entityService = entityService;
    }

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return new SprintFilterFactory(multiple);
    }

    private class SprintFilterFactory implements FilterFactory, ContextAware {

        private Context context;
        private boolean multiple;

        public SprintFilterFactory(boolean multiple) {
            this.multiple = multiple;
        }

        @Override
        public void setContext(Context context) {
            this.context = context;
        }

        @Override
        public FilterChooser createChooser(String value) {
            ItemsProvider.Loader<ComboItem> provider;
            EntityFilter query = context.getEntityQuery();
            if(query != null && "release-backlog-item".equals(query.getEntityType())) {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        return FilterManager.asItems(sprintService.getSprints(), "id", multiple, false);
                    }
                };
            } else if(context.getEntity() != null) {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        EntityQuery query = new EntityQuery("release-cycle");
                        String releaseId = ReleaseType.getEditorValue(context, "release-backlog-item.release-id");
                        if(releaseId.isEmpty()) {
                            return Collections.emptyList();
                        } else {
                            query.setValue("parent-id", releaseId);
                            EntityList sprints = entityService.query(query);
                            return FilterManager.asItems(sprints, "id", multiple, false);
                        }
                    }
                };
            } else {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        EntityQuery query = new EntityQuery("release-cycle");
                        String releaseCondition = query.getValue("release-backlog-item.release-id");
                        query.setValue("parent-id", releaseCondition);
                        query.addColumn("id", 1);
                        query.addColumn("name", 1);
                        query.addColumn("parent-id", 1);
                        query.addColumn("release.name", 1);
                        EntityList sprints = entityService.query(query);
                        if((releaseCondition != null && releaseCondition.contains(";")) || multipleReleases(sprints)) {
                            return asItems(sprints, multiple);
                        } else {
                            return FilterManager.asItems(sprints, "id", multiple, false);
                        }
                    }
                };
            }
            return new MultipleItemsFactory(project, "Sprint", multiple, provider, translateService.getReferenceTranslator("release-cycle")).createChooser(value);
        }

        @Override
        public List<String> getCustomChoices() {
            return null;
        }

        private boolean multipleReleases(List<Entity> sprints) {
            HashSet<String> releaseIds = new HashSet<String>();
            for(Entity sprint: sprints) {
                releaseIds.add(sprint.getPropertyValue("parent-id"));
                if(releaseIds.size() > 1) {
                    return true;
                }
            }
            return false;
        }

        private List<ComboItem> asItems(List<Entity> list, boolean multiple) {
            ArrayList<ComboItem> items = new ArrayList<ComboItem>();
            for(Entity item: list) {
                items.add(new ComboItem(item.getPropertyValue("id"), item.getPropertyValue("name") + " (" + item.getPropertyValue("release.name") + ")"));
            }
            items.add(new ComboItem(multiple? MultipleItemsTranslatedResolver.NO_VALUE: "", MultipleItemsTranslatedResolver.NO_VALUE_DESC));
            return items;
        }

    }
}
