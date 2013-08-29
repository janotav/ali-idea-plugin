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

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;

public class WarningPanel extends JPanel {

    private JComponent component;

    public WarningPanel(String warning, Color background, boolean canClose, boolean visible) {
        this(new JLabel(warning), background, canClose, visible);
    }

    public WarningPanel(JComponent comp, Color background, boolean canClose, boolean visible) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 2));

        this.component = comp;

        comp.setBackground(Color.YELLOW);
        setBackground(Color.YELLOW);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 2, 0, background), BorderFactory.createEtchedBorder()));
        add(new JLabel(IconLoader.getIcon("/general/balloonInformation.png")));
        add(comp);
        if(canClose) {
            LinkLabel warningCloseLink = new LinkLabel("", IconLoader.getIcon("/actions/closeNew.png"));
            warningCloseLink.setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel linkLabel, Object o) {
                    setVisible(false);
                }
            }, null);
            add(warningCloseLink);
        }
        setVisible(visible);
    }

    public JComponent getComponent() {
        return component;
    }
}
