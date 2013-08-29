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

import com.hp.alm.ali.idea.action.attachment.AttachmentOpenAction;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class AttachmentTableLoader extends QueryTableLoader {

    public AttachmentTableLoader(Project project, Entity entity, EntityQuery query, Set<String> hiddenFields) {
        super(project, entity, query, hiddenFields);
    }

    @Override
    protected void onTableCreated(final EntityTable entityTable) {
        entityTable.getTable().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int selectedRow = entityTable.getTable().getSelectedRow();
                    if(selectedRow >= 0) {
                        int idx = entityTable.getTable().convertRowIndexToModel(selectedRow);
                        Entity entity = entityTable.getModel().getEntity(idx);
                        int size = Integer.valueOf(entity.getPropertyValue("file-size"));
                        String name = entity.getPropertyValue("name");
                        if(AttachmentOpenAction.isAllowed(name, size)) {
                            EntityRef parent = new EntityRef(entity.getPropertyValue("parent-type"), Integer.valueOf(entity.getPropertyValue("parent-id")));
                            AttachmentOpenAction.openAttachment(project, name, parent, size);
                        }
                    }
                }
            }
        });
    }
}
