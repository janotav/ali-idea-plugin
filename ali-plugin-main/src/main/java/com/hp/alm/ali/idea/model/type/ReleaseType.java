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

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.filter.MultipleItemsChooserFactory;
import com.hp.alm.ali.idea.filter.MultipleItemsFactory;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.TeamService;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.ui.editor.field.EditableField;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

public class ReleaseType extends ReferenceType {

    private EntityService entityService;
    private TeamService teamService;

    public ReleaseType(Project project, EntityService entityService, TeamService teamService) {
        super(project, "release");
        this.entityService = entityService;
        this.teamService = teamService;
    }

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return new ReleaseFilterFactory(multiple);
    }

    private class ReleaseFilterFactory extends MultipleItemsFactory implements ContextAware {

        private Context context;
        private boolean multiple;

        public ReleaseFilterFactory(boolean multiple) {
            this.multiple = multiple;
        }

        @Override
        public void setContext(Context context) {
            this.context = context;
        }

        @Override
        public FilterChooser createChooser(String value) {
            ItemsProvider.Loader<ComboItem> provider = new ItemsProvider.Loader<ComboItem>() {
                @Override
                public List<ComboItem> load() {
                    EntityQuery query = new EntityQuery("release");
                    EntityList releases = entityService.query(query);
                    return FilterManager.asItems(releases, "id", multiple, false);
                }
            };
            FilterChooser releaseChooser = new MultipleItemsChooserFactory(project, "Release", multiple, provider).createChooser(value);
            if(context != null) {
                EntityEditor editor = context.getEntityEditor();
                if(editor != null) {
                    return new FilterChooserWrapper(editor, releaseChooser);
                }
            }
            return releaseChooser;
        }

        private class FilterChooserWrapper implements FilterChooser {

            private EntityEditor editor;
            private FilterChooser chooser;

            public FilterChooserWrapper(EntityEditor editor, FilterChooser chooser) {
                this.editor = editor;
                this.chooser = chooser;
            }

            @Override
            public void show() {
                String oldValue = chooser.getSelectedValue();
                chooser.show();
                final String releaseId = chooser.getSelectedValue();
                if(!ObjectUtils.equals(oldValue, releaseId)) {
                    // sprints are not considered equal across release even if name matches, clear assignment (not done automatically)
                    editor.setFieldValue("release-backlog-item.sprint-id", "");

                    // NOTE: unlike sprint, team seems to be cleared automatically and clearing explicitly could be avoided (kept for the sake of clarity)
                    String teamId = getEditorValue(context, "release-backlog-item.team-id");
                    if(!StringUtils.isEmpty(teamId)) {
                        // for teams try to assign to the same team in the new release
                        if(!StringUtils.isEmpty(releaseId)) {
                            entityService.requestCachedEntity(new EntityRef("team", Integer.valueOf(teamId)), Collections.singletonList("name"), new EntityListener() {
                                @Override
                                public void entityLoaded(Entity entity, Event event) {
                                    final Entity team = teamService.getTeam(entity.getPropertyValue("name"), Integer.valueOf(releaseId));
                                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(team != null) {
                                                editor.setFieldValue("release-backlog-item.team-id", team.getPropertyValue("id"));
                                            } else {
                                                // team with this name does not exist in the new release
                                                editor.setFieldValue("release-backlog-item.team-id", "");
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void entityNotFound(EntityRef ref, boolean removed) {
                                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                                        @Override
                                        public void run() {
                                            // original team no longer exists, probably rare
                                            editor.setFieldValue("release-backlog-item.team-id", "");
                                        }
                                    });
                                }
                            });
                        } else {
                            // no release, clear team assignment
                            editor.setFieldValue("release-backlog-item.team-id", "");
                        }
                    }
                }
            }

            @Override
            public String getSelectedValue() {
                return chooser.getSelectedValue();
            }
        }
    }

    public static String getEditorValue(Context context, String property) {
        EntityEditor editor = context.getEntityEditor();
        EditableField field = editor.getField(property);
        if(field != null) {
            return field.getValue();
        } else {
            return context.getEntity().getPropertyValue(property);
        }
    }
}
