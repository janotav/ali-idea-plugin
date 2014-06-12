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

package com.hp.alm.ali.idea.content.backlog;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.content.EntityContentPanel;
import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.ui.ReleaseChooser;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.ui.GotoEntityField;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BacklogPanel extends JPanel implements SprintService.Listener, EntityQueryProcessor {

    public static final String PLACE = "HPALI.ReleaseBacklog";

    private EntityContentPanel backlogItemsPanel;
    private EntityTableModel model;
    private Project project;
    private SprintService sprintService;

    public BacklogPanel(Project project) {
        super(new BorderLayout());

        this.project = project;

        sprintService = project.getComponent(SprintService.class);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        toolbar.add(ActionUtil.createActionToolbar("hpali.backlog", PLACE, true).getComponent());
        backlogItemsPanel = new EntityContentPanel(project, "release-backlog-item", toolbar, Collections.singleton("release-id"), this);
        backlogItemsPanel.setEntityAction(new ForwardBacklogAction());
        final EntityTable entityTable = backlogItemsPanel.getEntityTable();
        model = entityTable.getModel();
        entityTable.setDataProvider(new DataProvider() {
            @Override
            public Object getData(@NonNls String s) {
                if("entity-list".equals(s)) {
                    List<Entity> list = new LinkedList<Entity>();
                    int[] rows = entityTable.getTable().getSelectedRows();
                    for(int row: rows) {
                        Entity entity = model.getEntity(entityTable.getTable().convertRowIndexToModel(row));
                        Entity workItem = new Entity(entity.getPropertyValue("entity-type"), Integer.valueOf(entity.getPropertyValue("entity-id")));
                        workItem.mergeRelatedEntity(entity);
                        list.add(workItem);
                    }
                    return list;
                }
                return null;
            }
        });
        model.setMatcher(new EntityTableModel.EntityMatcher() {
            @Override
            public boolean matches(Entity created) {
                Entity release = sprintService.getRelease();
                return release != null && release.getPropertyValue("id").equals(created.getPropertyValue("release-id"));
            }
        });
        sprintService.addListener(this); // handler references model, must be already initialized

        toolbar.add(new ReleaseChooser(project));
        toolbar.add(new JLabel("Story:"));
        toolbar.add(new GotoEntityField("requirement", new BackwardBacklogAction()));
        toolbar.add(new JLabel("Defect:"));
        toolbar.add(new GotoEntityField("defect", new BackwardBacklogAction()));

        add(backlogItemsPanel, BorderLayout.CENTER);
    }

    @Override
    public void onReleaseSelected(Entity release) {
        model.reload();
    }

    @Override
    public void onSprintSelected(Entity sprint) {
    }

    @Override
    public void onTeamSelected(Entity team) {
    }

    @Override
    public EntityQuery preProcess(EntityQuery query) {
        Entity release = sprintService.getRelease();
        if(release == null) {
            return null;
        } else {
            EntityQuery clone = query.clone();
            clone.setValue("release-id", release.getPropertyValue("id"));
            return clone;
        }
    }

    public class ForwardBacklogAction implements EntityContentPanel.EntityAction {

        @Override
        public void perform(Entity entity) {
            Entity target = new Entity(entity.getPropertyValue("entity-type"), Integer.valueOf(entity.getPropertyValue("entity-id")));
            // TODO: copy existing compound properties
            AliContentFactory.loadDetail(project, target, true, true);
            backlogItemsPanel.scrollTo(entity);
        }
    }

    public class BackwardBacklogAction implements EntityContentPanel.EntityAction {

        @Override
        public void perform(Entity entity) {
            AliContentFactory.loadDetail(project, entity, true, true);

            EntityTableModel model = backlogItemsPanel.getEntityTable().getModel();
            int n = model.getRowCount();
            for(int i = 0; i < n; i++) {
                Entity bli = model.getEntity(i);
                if(entity.getType().equals(bli.getPropertyValue("entity-type")) && entity.getPropertyValue("id").equals(bli.getPropertyValue("entity-id")))  {
                    backlogItemsPanel.scrollTo(bli);
                }
            }
        }
    }
}
