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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Color;

public class SprintWarningPanel extends WarningPanel implements HyperlinkListener, SprintService.Listener {

    private SprintService sprintService;

    public SprintWarningPanel(Project project, Color background, boolean canClose) {
        super(HTMLAreaField.createTextPane(""), background, canClose, false);

        this.sprintService = project.getComponent(SprintService.class);

        setVisible(false);

        ((JTextPane)getComponent()).addHyperlinkListener(this);
        sprintService.addListener(this);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            Entity sprint = sprintService.getCurrentSprint();
            if(sprint != null) {
                sprintService.selectSprint(sprint);
            }
        }
    }

    @Override
    public void onReleaseSelected(Entity release) {
    }

    @Override
    public void onSprintSelected(final Entity sprint) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if(sprint != null && !SprintService.isCurrentSprint(sprint)) {
                    ((JTextPane)getComponent()).setText(getTextValue(sprintService.getCurrentSprint()));
                    setVisible(true);
                } else {
                    setVisible(false);
                }
            }
        });
    }

    @Override
    public void onTeamSelected(Entity team) {
    }

    private static String getTextValue(Entity sprint) {
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body>Currently selected sprint is not the current sprint of the release.");
        if(sprint != null) {
            buf.append(" Select <a href=\"");
            buf.append("current_sprint");
            buf.append("\">");
            buf.append(sprint.getPropertyValue("name"));
            buf.append("</a></body></html>.");
        }
        buf.append("</body></html>.");
        return buf.toString();
    }
}
