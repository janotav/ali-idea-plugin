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

package com.hp.alm.ali.idea.ui.entity.query;

import com.hp.alm.ali.idea.entity.EntityCrossFilter;
import com.hp.alm.ali.idea.model.Metadata;
import com.intellij.openapi.project.Project;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Set;

public class EntityCrossFilterDialog extends EntityFilterDialog<EntityCrossFilter> {

    public EntityCrossFilterDialog(Project project, String entityType, EntityCrossFilter query, String title, Set<String> hiddenFields) {
        super(project, entityType, query, title, hiddenFields);
    }

    protected JPanel createFilterTable(final EntityCrossFilter query, final Metadata metadata) {
        JPanel panel = super.createFilterTable(query, metadata);
        final JCheckBox inclusiveBox = new JCheckBox("Inclusive Filter", query.isInclusive());
        inclusiveBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                query.setInclusive(inclusiveBox.isSelected());
                queryModel.fireFilterUpdated(true);
            }
        });
        panel.add(inclusiveBox, BorderLayout.SOUTH);
        return panel;
    }
}
