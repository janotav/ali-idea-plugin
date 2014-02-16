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

import com.hp.alm.ali.idea.entity.tree.HierarchicalEntityModel;
import com.hp.alm.ali.idea.entity.tree.HierarchicalModelFactory;
import com.intellij.openapi.project.Project;

import java.awt.BorderLayout;

public class HierarchicalChooser extends EntityChooser {

    private HierarchicalEntityModel treeModel;
    private TreePanel treePanel;

    public HierarchicalChooser(Project project, String entityType, boolean hideRoot, boolean multiple, boolean idSelection, final boolean acceptEmpty, HierarchicalEntityModel treeModel) {
        super(project, entityType, multiple, acceptEmpty);

        if(treeModel == null) {
            this.treeModel = project.getComponent(HierarchicalModelFactory.class).createModel(entityType);
        } else {
            this.treeModel = treeModel;
        }
        treePanel = new TreePanel(project, this.treeModel);
        if(multiple && !idSelection) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new AppendingTreeListener(valueField, entityType));
        } else if(!multiple && !idSelection) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new FollowTreeListener(valueField, entityType));
        } else if(!multiple) {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new IdFollowTreeListener(valueField, entityType));
        } else {
            treePanel.getTree().getSelectionModel().addTreeSelectionListener(new IdAppendingTreeListener(valueField, entityType));
        }
        treePanel.getTree().setRootVisible(!hideRoot);
        getContentPane().add(treePanel, BorderLayout.CENTER);
    }
}
