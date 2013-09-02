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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.ScrollablePanel;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public class EntityDetail extends JPanel implements ContentManagerListener, HasEntity, EntityListener, ServerTypeListener, DataProvider {
    private RestService restService;
    private EntityService entityService;
    private PropertyGrid entityGrid;
    private boolean selected;
    private PageBar pageBar;
    private AliProjectConfiguration conf;

    public EntityDetail(Project project, Entity entity) {
        super(new BorderLayout());
        this.conf = project.getComponent(AliProjectConfiguration.class);

        // toolbar
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.entity", "detail", false);
        actionToolbar.setTargetComponent(this);
        add(actionToolbar.getComponent(), BorderLayout.NORTH);

        // extra panel
        ExtraPanel extraContent = new ExtraPanel();
        extraContent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
        pageBar = new PageBar(project, entity);
        pageBar.setExtraContent(extraContent);
        JPanel bottomPanel = new RestrainedPanel(new BorderLayout());
        bottomPanel.add(extraContent, BorderLayout.CENTER);
        bottomPanel.add(pageBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // content
        this.entityGrid = new PropertyGrid(project, entity);
        JPanel outer = new ScrollablePanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.add(entityGrid, BorderLayout.CENTER);

        ScrollablePanel contentNoExpansion = new ScrollablePanel(new BorderLayout());
        contentNoExpansion.setBackground(Color.WHITE);
        contentNoExpansion.add(outer, BorderLayout.NORTH);
        add(new JBScrollPane(contentNoExpansion), BorderLayout.CENTER);

        restService = project.getComponent(RestService.class);
        restService.addServerTypeListener(this);

        entityService = project.getComponent(EntityService.class);
        entityService.addEntityListener(this);
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    pageBar.initialize();
                    if(selected) {
                        pageBar.reload();
                    }
                }
            });
        }
    }

    @Override
    public void entityLoaded(Entity entity, Event event) {
        if (entity.equals(entityGrid.getEntity()) && event == Event.REFRESH) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    if(selected) {
                        pageBar.reload();
                    } else {
                        pageBar.markForReload();
                    }
                }
            });
        }
    }

    @Override
    public void entityNotFound(EntityRef ref, boolean removed) {
        if (ref.equals(new EntityRef(entityGrid.getEntity()))) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    pageBar.setVisible(false);
                }
            });
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height += pageBar.getPreferredHeightWhenExpanded();
        return size;
    }

    @Override
    public void contentAdded(ContentManagerEvent contentManagerEvent) {
    }

    @Override
    public void contentRemoved(ContentManagerEvent contentManagerEvent) {
        if(this.equals(contentManagerEvent.getContent().getComponent())) {
            contentManagerEvent.getContent().getManager().removeContentManagerListener(this);
            _unregister();
        }
    }

    /*
     * method should be considered private except for usage in tests
     */
    public void _unregister() {
        entityGrid.remove();
        pageBar.remove();
        entityService.removeEntityListener(this);
        restService.removeServerTypeListener(this);
    }

    @Override
    public void contentRemoveQuery(ContentManagerEvent contentManagerEvent) {
        // we want the "cancel" button to apply to all modified content
        // and thus need central point (EntityEditManager) to listen to removal query
    }

    @Override
    public void selectionChanged(ContentManagerEvent contentManagerEvent) {
        Content selectedContent = contentManagerEvent.getContent().getManager().getSelectedContent();
        updateSelection(selectedContent);
    }

    @Override
    public Entity getEntity() {
        return entityGrid.getEntity();
    }

    public void updateSelection(Content selectedContent) {
        selected = selectedContent != null && this.equals(selectedContent.getComponent());
        if(selected) {
            pageBar.reloadIfNeeded();
            conf.getDetails().setSelectedRef(new EntityRef(entityGrid.getEntity()));
        }
    }

    @Override
    public Object getData(@NonNls String s) {
        if("entity-list".equals(s)) {
            return Arrays.asList(entityGrid.getEntity());
        }
        return null;
    }

    private class RestrainedPanel extends JPanel  {

        public RestrainedPanel(LayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.height = Math.min(EntityDetail.this.getHeight() - EntityDetail.this.getComponent(0).getHeight(), dim.height);
            return dim;
        }
    }

    private class ExtraPanel extends JPanel implements ExtraContent {

        private Component current;

        public ExtraPanel() {
            super(new BorderLayout());

            setBorder(new EmptyBorder(5, 5, 0, 0));
        }

        @Override
        public boolean toggleComponent(Component comp) {
            removeAll();
            if(current != comp) {
                add(new NonRootScrollPane(comp, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
                current = comp;
            } else {
                current = null;
            }
            revalidate();
            return current == comp;
        }

        @Override
        public void hideComponent(Component comp) {
            if(current == comp) {
                removeAll();
                current = null;
            }
        }
    }

    private static class NonRootScrollPane extends JBScrollPane {

        public NonRootScrollPane(Component comp, int vsbPolicy, int hsbPolicy) {
            super(comp, vsbPolicy, hsbPolicy);
        }

        @Override
        public boolean isValidateRoot() {
            return false;
        }
    }
}
