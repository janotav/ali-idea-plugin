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

package com.hp.alm.ali.idea.content.requirements;

import com.hp.alm.ali.idea.content.EntityContentPanel;
import com.hp.alm.ali.idea.ui.chooser.PopupDialog;
import com.hp.alm.ali.idea.content.AliContent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class RequirementsContent implements AliContent {

    private static RequirementsContent instance = new RequirementsContent();

    public static RequirementsContent getInstance() {
        return instance;
    }

    private static Icon gotoIcon = IconLoader.getIcon("/goto_16.png");

    @Override
    public String getName() {
        return "Requirements";
    }

    @Override
    public JComponent create(final Project project) {
        JPanel requirementToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        LinkLabel gotoRequirement = new LinkLabel("", gotoIcon);
        requirementToolbar.add(gotoRequirement);
        final EntityContentPanel requirementPanel = new EntityContentPanel(project, "requirement", requirementToolbar);
        gotoRequirement.setListener(new LinkListener() {
            public void linkSelected(LinkLabel aSource, Object aLinkData) {
                PopupDialog popupDialog = new PopupDialog(project, "requirement", false, false, PopupDialog.Selection.FOLLOW_ID, false);
                popupDialog.setVisible(true);
                String idStr = popupDialog.getSelectedValue();
                if(!idStr.isEmpty()) {
                    requirementPanel.goTo(idStr);
                }
            }
        }, null);
        return requirementPanel;
    }
}
