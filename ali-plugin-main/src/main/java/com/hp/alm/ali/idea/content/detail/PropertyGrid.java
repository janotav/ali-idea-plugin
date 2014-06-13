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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.cfg.EntityFields;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.ui.viewer.NavigableViewer;
import com.hp.alm.ali.idea.ui.viewer.TranslatedViewer;
import com.hp.alm.ali.idea.ui.viewer.Viewer;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.cfg.EntityDetails;
import com.hp.alm.ali.idea.entity.CachingEntityListener;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.FieldNameLabel;
import com.hp.alm.ali.idea.ui.ScrollablePanel;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PropertyGrid extends ScrollablePanel implements EntityFields.ColumnsChangeListener, CachingEntityListener, ServerTypeListener, HasEntity {
    private Entity entity;
    private Map<String, LabelAndValue> map;
    private List<String> columns;

    private Project project;
    private EntityService entityService;
    private MetadataService metadataService;
    private TranslateService translateService;
    private RestService restService;
    private EntityDetails details;
    private EntityFields fields;
    private JComponent notFoundComponent;
    private GridBagLayout layout;

    private LinkListener moveUp = new MoveUpListener();
    private LinkListener moveDown = new MoveDownListener();
    private LinkListener remove = new RemoveListener();

    public PropertyGrid(Project project, Entity entity) {
        super(new GridBagLayout());

        this.project = project;
        this.entity = entity;

        layout = (GridBagLayout)getLayout();
        entityService = project.getComponent(EntityService.class);
        metadataService = project.getComponent(MetadataService.class);
        translateService = project.getComponent(TranslateService.class);
        restService = project.getComponent(RestService.class);
        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
        fields = conf.getFields(entity.getType());
        details = conf.getDetails();

        entityService.addEntityListener(this);
        restService.addServerTypeListener(this);

        setBackground(new JTextPane().getBackground());
        setBorder(new EmptyBorder(2, 2, 0, 0));

        map = new HashMap<String, LabelAndValue>();
        fields.addColumnsChangeListener(this);
        columns = fields.getColumns();

        updateColumns();
    }

    private boolean isFirst(String column) {
        return columns.indexOf(column) == 0;
    }

    private boolean isLast(String column) {
        return columns.indexOf(column) == columns.size() - 1;
    }

    public void columnsChanged(String columnToFocus) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        this.columns = fields.getColumns();
        if(!isNotFoundShowing()) {
            updateColumns();
        }
    }

    private GridBagConstraints getKeyConstraints(int idx, boolean longField) {
        GridBagConstraints clone = new GridBagConstraints();
        if(longField) {
            clone.gridwidth = 2;
            clone.weightx = 1;
        } else {
            clone.gridwidth = 1;
            clone.weightx = 0.0;
        }
        clone.gridx = 0;
        clone.gridy = idx;
        clone.fill = GridBagConstraints.HORIZONTAL;
        clone.anchor = GridBagConstraints.NORTH;
        return clone;
    }

    private GridBagConstraints getValueConstraints(int idx, boolean longField) {
        GridBagConstraints clone = new GridBagConstraints();
        if(longField) {
            clone.gridx = 0;
            clone.gridy = idx + 1;
            clone.gridwidth = 2;
            clone.weightx = 1;
        } else {
            clone.gridx = 1;
            clone.gridy = idx;
            clone.gridwidth = 1;
            clone.weightx = 1;
        }
        clone.anchor = GridBagConstraints.CENTER;
        clone.fill = GridBagConstraints.BOTH;
        return clone;
    }

    private void updateColumns() {
        metadataService.loadEntityMetadataAsync(entity.getType(), new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        updateColumns(metadata);
                    }
                });
            }

            @Override
            public void metadataFailed() {
            }
        });
    }

    private void updateColumns(Metadata metadata) {
        boolean needsRefresh = false;

        if(columns.isEmpty() && restService.getServerTypeIfAvailable().isConnected()) {
            List<String> defaultColumns = restService.getServerStrategy().getDefaultFields(entity.getType());
            if(!defaultColumns.isEmpty()) {
                fields.setColumns(defaultColumns);
                return; // will process the column changed event
            }
        }

        for(int i = 0; i < columns.size(); i++) {
            final String property = columns.get(i);
            String value = entity.getPropertyValue(property);

            if (!entity.isInitialized(property)) {
                if(!entity.isComplete() && metadata.getField(property) != null) {
                    value = "<html><body><i>Retrieving information...</i></body></html>";
                    needsRefresh = true;
                } else {
                    value = "<html><body><i>Couldn't retrieve value...</i></body></html>";
                }
            }

            String newValue = HTMLAreaField.removeSmallFont(value);

            final LabelAndValue labelAndValue = map.get(property);
            if(labelAndValue != null) {
                // update existing component

                labelAndValue.label.update();
                labelAndValue.value.setValue(newValue);

                shrinkOrExpand(i, labelAndValue.label, labelAndValue.value.getComponent());
            } else {
                // adding new column

                final ColumnLabelPanel panelWithLabel = new ColumnLabelPanel(property);
                final Viewer viewer;
                Field field = metadata.getField(property);
                if(field != null && translateService.isTranslated(field)) {
                    viewer = new TranslatedViewer(project, this, field, value);
                } else {
                    viewer = new NavigableViewer(project, value);
                }
                viewer.getComponent().addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        int i = columns.indexOf(property);
                        if (i >= 0) {
                            shrinkOrExpand(i, panelWithLabel, viewer.getComponent());
                        }
                    }
                });
                panelWithLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if(e.getClickCount() > 1 && layout.getConstraints(panelWithLabel).gridwidth > 1) {
                            viewer.getComponent().setVisible(!viewer.getComponent().isVisible());
                        }
                    }
                });

                add(panelWithLabel, getKeyConstraints(i * 2, false));
                add(viewer.getComponent(), getValueConstraints(i * 2, false));
                shrinkOrExpand(i, panelWithLabel, viewer.getComponent());
                map.put(property, new LabelAndValue(panelWithLabel, viewer));
            }
        }

        HashSet<String> all = new HashSet<String>(map.keySet());
        all.removeAll(columns);
        for(String column: all) {
            removeNamed(column);
        }

        // make sure DelegatingScrollPane size is adjusted as needed
        revalidate();
        repaint();

        if(needsRefresh) {
            entityService.getEntityAsync(new EntityRef(entity), null);
        }
    }

    private void shrinkOrExpand(int i, Component label, Component value) {
        Dimension size = value.getPreferredSize();
        int[][] dim = layout.getLayoutDimensions();
        if(dim.length == 0 || dim[0].length == 0) {
            return;
        }
        boolean idxChanged = layout.getConstraints(label).gridy != i * 2;
        if (size.height <= 20 && size.width + dim[0][0] <= getWidth() && (layout.getConstraints(value).gridwidth > 1 || idxChanged)) {
            // shrink back
            layout.setConstraints(label, getKeyConstraints(i * 2, false));
            layout.setConstraints(value, getValueConstraints(i * 2, false));
        } else if(size.height > 23 && (layout.getConstraints(value).gridwidth == 1 || idxChanged)) { // 23 is combo height
            // expand
            layout.setConstraints(label, getKeyConstraints(i * 2, true));
            layout.setConstraints(value, getValueConstraints(i * 2, true));
        }
    }

    private boolean isNotFoundShowing() {
        return notFoundComponent != null && notFoundComponent.getParent() != null;
    }

    private void removeNamed(String col) {
        LabelAndValue labelAndValue = map.remove(col);
        remove(labelAndValue.label);
        remove(labelAndValue.value.getComponent());
    }

    @Override
    public void entityLoaded(Entity entity, Event event) {
        if(entity.equals(this.entity)) {
            this.entity = entity;
        } else if("release-backlog-item".equals(entity.getType()) && this.entity.getType().equals(entity.getPropertyValue("entity-type")) && this.entity.getId() == Integer.valueOf(entity.getPropertyValue("entity-id"))) {
            this.entity.mergeRelatedEntity(entity);
        } else {
            return;
        }
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                if(isNotFoundShowing()) {
                    remove(notFoundComponent);
                }
                updateColumns();
            }
        });
    }

    @Override
    public void entityNotFound(EntityRef ref, boolean removed) {
        noEntity(ref, false, removed);
    }

    private void noEntity(EntityRef ref, final boolean loading, final boolean removed) {
        if(ref.equals(new EntityRef(entity))) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                public void run() {
                    if(removed) {
                        ToolWindow toolWindow = AliContentFactory.getDetailToolWindow(project);
                        toolWindow.getContentManager().removeContent(AliContentFactory.findContent(toolWindow, entity), true);
                    } else {
                        if(!isNotFoundShowing()) {
                            if(notFoundComponent == null) {
                                notFoundComponent = HTMLAreaField.createTextPane("<html><body><i>"+(loading?"Loading entity...":"Failed to load entity")+"</i></body></html>");
                            }

                            removeAll();
                            map.clear();
                            add(notFoundComponent);
                        }
                    }
                }
            });
        }
    }

    public Entity lookup(EntityRef ref) {
        if(ref.equals(new EntityRef(entity))) {
            return entity;
        } else {
            return null;
        }
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            entityService.getEntityAsync(new EntityRef(entity), null);
        } else {
            noEntity(new EntityRef(entity), serverType == ServerType.CONNECTING, false);
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void remove() {
        entityService.removeEntityListener(this);
        restService.removeServerTypeListener(this);
        fields.removeColumnsChangeListener(this);
        details.removeRef(new EntityRef(entity));
    }

    private class ColumnLabelPanel extends JPanel {

        private ColumnControls controls;
        private FieldNameLabel myLabel;

        public ColumnLabelPanel(String col) {
            super(new BorderLayout());

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, PropertyGrid.this.getBackground()), // align with single line values (that are higher by default)
                    BorderFactory.createEtchedBorder()));

            myLabel = new FieldNameLabel(entity.getType(), col, metadataService);
            myLabel.setHorizontalAlignment(SwingConstants.CENTER);

            add(myLabel, BorderLayout.WEST);

            controls = new ColumnControls(col);
            add(controls, BorderLayout.EAST);

            update();
        }

        public void update() {
            controls.update();
        }
    }

    private class ColumnControls extends JPanel {
        private LinkLabel moveDownLabel;
        private LinkLabel moveUpLabel;
        private LinkLabel deleteLabel;

        private String column;

        public ColumnControls(String col) {
            super(new GridLayout(1, 0, 5, 0));

            this.column = col;

            setBorder(new EmptyBorder(0, 0, 0, 5));

            moveDownLabel = new LinkLabel("", IconLoader.getIcon("/actions/sortDesc.png"), moveDown, col);
            add(moveDownLabel);
            moveUpLabel = new LinkLabel("", IconLoader.getIcon("/actions/sortAsc.png"), moveUp, col);
            add(moveUpLabel);
            deleteLabel = new LinkLabel("", IconLoader.getIcon("/modules/deleteContentFolder.png"), remove, col);
            add(deleteLabel);

            update();
        }

        public void update() {
            boolean show = details.isColumnControls();
            if(show) {
                boolean first = isFirst(column);
                boolean last = isLast(column);

                moveDownLabel.setVisible(!last);
                moveUpLabel.setVisible(!first);
                deleteLabel.setVisible(!(first && last));
                setVisible(true);
            } else {
                setVisible(false);
            }
        }
    }

    private class RemoveListener implements LinkListener {
        public void linkSelected(LinkLabel linkLabel, Object o) {
            fields.removeColumn((String)o);
        }
    }

    private class MoveDownListener implements LinkListener {
        public void linkSelected(LinkLabel linkLabel, Object o) {
            fields.moveColumn((String)o, 1);
        }
    }

    private class MoveUpListener implements LinkListener {
        public void linkSelected(LinkLabel linkLabel, Object o) {
            fields.moveColumn((String)o, -1);
        }
    }

    private static class LabelAndValue {
        PropertyGrid.ColumnLabelPanel label;
        Viewer value;

        public LabelAndValue(ColumnLabelPanel label, Viewer value) {
            this.label = label;
            this.value = value;
        }
    }
}
