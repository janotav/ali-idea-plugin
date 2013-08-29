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

package com.hp.alm.ali.idea.content.detail;

import com.hp.alm.ali.idea.action.link.LinkViewAction;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class LinksTableLoader implements TableContent.EntityTableLoader {

    private Project project;
    private Entity entity;
    private EntityQuery linkQuery;
    private final Set<String> hiddenFields;
    private EntityTable entityTable;

    public LinksTableLoader(Project project, Entity entity, EntityQuery linkQuery, Set<String> hiddenFields) {
        this.project = project;
        this.entity = entity;
        this.linkQuery = linkQuery;
        this.hiddenFields = hiddenFields;
    }

    @Override
    public void load(TableContent content) {
        if(entityTable == null) {
            EntityQuery query = project.getComponent(AliProjectConfiguration.class).getFilter(linkQuery.getEntityType());
            linkQuery.setColumns(query.getColumns());
            entityTable = new EntityTable(project, linkQuery.getEntityType(), false, linkQuery, hiddenFields, null, false, entity);
            entityTable.getModel().setMatcher(new EntityTableModel.EntityMatcher() {
                @Override
                public boolean matches(Entity created) {
                    if ("defect".equals(entity.getType()) && created.getPropertyValue("first-endpoint-id").equals(entity.getPropertyValue("id"))) {
                        return true;
                    } else if (created.getPropertyValue("second-endpoint-type").equals(entity.getType()) && created.getPropertyValue("second-endpoint-id").equals(entity.getPropertyValue("id"))) {
                        return true;
                    }
                    return false;
                }
            });
            entityTable.getTable().addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        int selectedRow = entityTable.getTable().getSelectedRow();
                        if (selectedRow >= 0) {
                            int idx = entityTable.getTable().convertRowIndexToModel(selectedRow);
                            Entity entity = entityTable.getModel().getEntity(idx);
                            LinkViewAction.openLinkedEntity(project, entity, LinksTableLoader.this.entity);
                        }
                    }
                }
            });
        }
        content.setTable(entityTable);
    }
}
