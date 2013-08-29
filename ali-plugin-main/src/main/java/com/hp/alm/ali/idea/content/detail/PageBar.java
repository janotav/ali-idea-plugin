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

import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class PageBar extends JPanel implements ServerTypeListener {

    private List<MyContentButton> components;
    private ButtonGroup group;
    private ExtraContent extraContent;
    private JToggleButton unselector;
    private RestService restService;
    private boolean reloadNeeded;
    private Entity entity;

    public PageBar(Project project, Entity entity) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.entity = entity;

        components = new LinkedList<MyContentButton>();

        group = new ButtonGroup();
        unselector = new JToggleButton();
        group.add(unselector);

        restService = project.getComponent(RestService.class);

        initialize();

        restService.addServerTypeListener(this);
    }

    public int getPreferredHeightWhenExpanded() {
        return Collections.max(components, new Comparator<JComponent>() {
            @Override
            public int compare(JComponent o1, JComponent o2) {
                return o1.getPreferredSize().height - o2.getPreferredSize().height;
            }
        }).getPreferredSize().height;
    }

    public void markForReload() {
        reloadNeeded = true;
    }

    public void reloadIfNeeded() {
        if(reloadNeeded) {
            reload();
        }
    }

    private void removeExistingComponents() {
        for(MyContentButton contentButton: components) {
            group.remove(contentButton);
            contentButton.getContent().remove();
            remove(contentButton);
        }
        components.clear();
    }

    public void remove() {
        restService.removeServerTypeListener(this);
        removeExistingComponents();
    }

    public void initialize() {
        removeExistingComponents();

        List<DetailContent> list = restService.getModelCustomization().getDetailContent(entity);
        for(DetailContent content: list) {
            addContent(content);
        }

        revalidate();
        repaint();

        reloadNeeded = true;
    }

    public void reload() {
        reloadNeeded = false;
        setVisible(true);
        for(MyContentButton contentButton: components) {
            contentButton.getContent().reload();
        }
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        extraContent.setVisible(visible);
    }

    public void addContent(final DetailContent content) {
        final MyContentButton contentButton = new MyContentButton(content, null, content.getIcon());
        components.add(contentButton);
        group.add(contentButton);
        contentButton.setFocusable(false);
        contentButton.addActionListener(new ToggleListener(content));
        contentButton.getContent().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        contentButton.setEnabled(content.getComponent() != null && restService.getServerTypeIfAvailable().isConnected());
                        contentButton.setText(content.getLinkText());
                    }
                });
            }
        });
        contentButton.setEnabled(content.getComponent() != null && restService.getServerTypeIfAvailable().isConnected());
        contentButton.setText(content.getLinkText());
        add(contentButton);
    }

    public void setExtraContent(ExtraContent extraContent) {
        this.extraContent = extraContent;
    }

    @Override
    public void connectedTo(final ServerType serverType) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                boolean connected = serverType.isConnected();
                if(!connected) {
                    group.clearSelection();
                }
                for(MyContentButton button: components) {
                    if(!connected) {
                        extraContent.hideComponent(button.getContent().getComponent());
                    }
                    button.setEnabled(connected);
                }
            }
        });
    }

    private class ToggleListener implements ActionListener {
        private DetailContent content;

        public ToggleListener(DetailContent content) {
            this.content = content;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if(!extraContent.toggleComponent(content.getComponent())) {
                unselector.setSelected(true);
            }
        }
    }

    private static class MyContentButton extends JToggleButton {
        private DetailContent content;

        public MyContentButton(DetailContent content, String label, Icon icon) {
            super(label, icon);

            this.content = content;
        }

        public DetailContent getContent() {
            return content;
        }
    }

}
