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
import com.hp.alm.ali.idea.filter.MultipleItemsChooserFactory;
import com.hp.alm.ali.idea.filter.MultipleItemsFactory;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsResolver;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsTranslatedResolver;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.services.TeamService;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.editor.BaseEditor;
import com.hp.alm.ali.idea.ui.editor.field.EditableField;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TeamType extends ReferenceType {

    private SprintService sprintService;
    private EntityService entityService;
    private TeamService teamService;

    public TeamType(Project project, SprintService sprintService, EntityService entityService, TeamService teamService) {
        super(project, "team");
        this.sprintService = sprintService;
        this.entityService = entityService;
        this.teamService = teamService;
    }

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return new TeamFilterFactory(multiple);
    }

    @Override
    public FilterResolver getFilterResolver() {
        return new MultipleItemsResolver() {
            @Override
            public String toRESTQuery(String value) {
                List<String> values = new LinkedList<String>(Arrays.asList(value.split(";")));
                LinkedList<String> ids = new LinkedList<String>();
                if(values.remove(MultipleItemsTranslatedResolver.NO_VALUE)) {
                    ids.add(MultipleItemsTranslatedResolver.NO_VALUE);
                }
                if(!values.isEmpty()) {
                    EntityList teams = teamService.getMultipleTeams(values);
                    ids.addAll(teams.getIdStrings());
                }
                return StringUtils.join(ids, " OR ");
            }
        };
    }

    private class TeamFilterFactory extends MultipleItemsFactory implements ContextAware {

        private boolean multiple;
        private Context context;

        public TeamFilterFactory(boolean multiple) {
            this.multiple = multiple;
        }

        @Override
        public FilterChooser createChooser(String value) {
            ItemsProvider<ComboItem> provider;
            EntityFilter query = context.getEntityQuery();
            if(query != null && "release-backlog-item".equals(query.getEntityType())) {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        return FilterManager.asItems(sprintService.getTeams(), "name", multiple, false);
                    }
                };
            } else if(context.getEntity() != null) {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        EntityQuery query = new EntityQuery("team");
                        BaseEditor editor = context.getEntityEditor();
                        EditableField field = editor.getField("release-backlog-item.release-id");
                        String releaseId;
                        if(field != null) {
                            releaseId = field.getValue();
                        } else {
                            releaseId = context.getEntity().getPropertyValue("release-backlog-item.release-id");
                        }
                        if(releaseId.isEmpty()) {
                            return Collections.emptyList();
                        } else {
                            query.setValue("release.id", releaseId);
                            query.setPropertyResolved("release.id", true);
                            EntityList sprints = entityService.query(query);
                            return FilterManager.asItems(sprints, "id", multiple, false);
                        }
                    }
                };
            } else {
                provider = new ItemsProvider.Loader<ComboItem>() {
                    @Override
                    public List<ComboItem> load() {
                        EntityQuery query = new EntityQuery("team");
                        String releaseCondition = context.getEntityQuery().getValue("release-backlog-item.release-id");
                        query.setValue("release.id", releaseCondition);
                        query.setPropertyResolved("release.id", true);
                        query.addColumn("id", 1);
                        query.addColumn("name", 1);
                        query.addColumn("description", 1);
                        query.addColumn("release-id", 1);
                        query.addOrder("name", SortOrder.ASCENDING);
                        EntityList teams = entityService.query(query);
                        Set<String> names = new HashSet<String>();
                        for(Entity team: teams) {
                            names.add(team.getPropertyValue("name"));
                        }
                        return FilterManager.asItems(new ArrayList<String>(names), multiple, false);
                    }
                };
            }
            return new MultipleItemsChooserFactory(project, "Team", multiple, provider).createChooser(value);
        }

        @Override
        public void setContext(Context context) {
            this.context = context;
        }
    }
}
