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

package com.hp.alm.ali.idea.content.defect;

import com.hp.alm.ali.idea.content.EntityContentPanel;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.content.AliContent;
import com.hp.alm.ali.idea.ui.GotoEntityField;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class DefectsContent implements AliContent {

    private static DefectsContent instance = new DefectsContent();

    public static DefectsContent getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Defects";
    }

    @Override
    public JComponent create(final Project project) {
        JPanel defectToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.defects", ActionPlaces.UNKNOWN, true);
        defectToolbar.add(actionToolbar.getComponent());
        EntityContentPanel defectPanel = new EntityContentPanel(project, "defect", defectToolbar);
        actionToolbar.setTargetComponent(defectPanel.getEntityTable());
        defectToolbar.add(new GotoEntityField("defect", defectPanel.getEntityAction()));

        return defectPanel;
    }
}
