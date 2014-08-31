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
import com.intellij.openapi.ui.JBCheckboxMenuItem;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
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
    private JPanel centerPanel;
    private JButton moreLink;

    public PageBar(Project project, Entity entity) {
        super(new GridBagLayout());
        this.entity = entity;

        createCenterPanelAndMoreLink();
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

    private void createCenterPanelAndMoreLink() {
        centerPanel = new JPanel(new ProxyFlowLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        add(centerPanel, c);
        moreLink = new JButton(IconLoader.getIcon("/ide/link.png"));
        moreLink.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        moreLink.addActionListener(new MoreLinkActionListener());
        moreLink.setVisible(false);
        ++c.gridx;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.weightx = 0;
        add(moreLink, c);
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

        List<DetailContent> list = restService.getServerStrategy().getDetailContent(entity);
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
        contentButton.addActionListener(new ToggleListener(contentButton));
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
        centerPanel.add(contentButton);
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
        private MyContentButton contentButton;

        public ToggleListener(MyContentButton contentButton) {
            this.contentButton = contentButton;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if(!extraContent.toggleComponent(contentButton.getContent().getComponent())) {
                unselector.setSelected(true);
            } else {
                contentButton.setSelected(true);
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

    private class MoreLinkActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final JPopupMenu menu = new JPopupMenu();
            int n = centerPanel.getComponentCount();
            if (n > 1) {
                Component firstComponent = centerPanel.getComponent(0);
                for (int i = 1; i < n; i++) {
                    final MyContentButton component = (MyContentButton) centerPanel.getComponent(i);
                    if (component.getY() > firstComponent.getY()) {
                        JBCheckboxMenuItem item = new JBCheckboxMenuItem(component.getText());
                        item.setState(component.isSelected());
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                menu.setVisible(false);
                                new ToggleListener(component).actionPerformed(e);
                            }
                        });
                        menu.add(item);
                    }
                }
            }
            menu.show(PageBar.this, moreLink.getX(), moreLink.getY());
        }
    }

    private class ProxyFlowLayout extends FlowLayout {

        private ProxyFlowLayout() {
            super(FlowLayout.LEFT);
        }

        @Override
        public void layoutContainer(Container target) {
            super.layoutContainer(target);
            int n = centerPanel.getComponentCount();
            if (n > 1) {
                if (centerPanel.getComponent(n - 1).getY() > centerPanel.getComponent(0).getY()) {
                    if (!moreLink.isVisible()) {
                        moreLink.setVisible(true);
                        // if the resize event is small enough, the link wouldn't (sometimes) show until next event
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                PageBar.this.revalidate();
                                PageBar.this.repaint();
                            }
                        });
                    }
                    return;
                }
            }
            moreLink.setVisible(false);
        }
    }
}
