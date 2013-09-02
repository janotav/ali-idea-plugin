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

import com.hp.alm.ali.idea.entity.EntityFilterModel;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.entity.FilterModelImpl;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

public class EntityQueryPicker extends JPanel {
    private EntityQueryPanel queryPanel;
    private JButton browse;
    private EntityFilterModel<EntityQuery> queryModel;

    public EntityQueryPicker(final Project project, final EntityQuery filter, final String entityType) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        queryModel = new FilterModelImpl<EntityQuery>(filter);
        queryPanel = new EntityQueryPanel(project, queryModel, entityType, Collections.<String>emptySet(), ";", true, false, false, false);
        queryPanel.setPreferredSize(new Dimension(200, 18));
        queryPanel.setMinimumSize(queryPanel.getPreferredSize());
        queryPanel.setMaximumSize(queryPanel.getPreferredSize());
        queryPanel.setSize(queryPanel.getPreferredSize());
        queryPanel.setBorder(BorderFactory.createEtchedBorder());
        add(queryPanel);

        browse = new JButton("...");
        browse.setPreferredSize(new Dimension(25, 19));
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EntityQuery updatedFilter = new EntityQueryDialog(project, entityType, filter, "Filter:", Collections.<String>emptySet()).chooseQuery();
                if(updatedFilter != null) {
                    filter.copyFrom(updatedFilter);
                    queryModel.fireFilterUpdated(true);
                }
            }
        });
        add(browse);

        add(Box.createHorizontalGlue());
    }

    public void addListener(FilterListener<EntityQuery> listener) {
        queryModel.addFilterListener(listener);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        queryPanel.setEnabled(enabled);
        browse.setEnabled(enabled);
    }
}
