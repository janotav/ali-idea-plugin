/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.ui.entity.table.EntityTable;
import com.intellij.openapi.project.Project;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Collections;

public class FlatChooser extends EntityChooser {

    private EntityTable entityTable;

    public FlatChooser(Project project, String entityType, boolean multiple, boolean acceptEmpty, EntityQueryProcessor processor) {
        super(project, entityType, multiple, acceptEmpty);

        EntityQuery filter = project.getComponent(AliProjectConfiguration.class).getLookupFilter(entityType);
        entityTable = new EntityTable(project, entityType, true, filter, Collections.<String>emptySet(), processor, true, null);
        if(multiple) {
            entityTable.getTable().getSelectionModel().addListSelectionListener(new AppendingListListener(valueField, entityTable.getModel()));
        } else {
            entityTable.getTable().getSelectionModel().addListSelectionListener(new IdFollowListListener(valueField, entityTable.getModel()));
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(entityTable, BorderLayout.CENTER);
        panel.add(entityTable.getStatusComponent(), BorderLayout.SOUTH);
        getContentPane().add(panel, BorderLayout.CENTER);
    }
}
