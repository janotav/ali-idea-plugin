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

package com.hp.alm.ali.idea.ui.dialog;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.ChangesetPanel;
import com.hp.alm.ali.idea.ui.QuickSearchPanel;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChangesDialog extends MyDialog implements ServerTypeListener, QuickSearchPanel.Target {

    private Project project;
    private RestService restService;
    private EntityService entityService;
    private MetadataService metadataService;
    private EntityRef entity;

    private JPanel contentPanel;
    private JPanel filler;
    private QuickSearchPanel quickSearch;

    private Map<Integer, ChangesetPanel> map = new HashMap<Integer, ChangesetPanel>();

    public ChangesDialog(final Project project, final EntityRef entity) {
        super(project, JOptionPane.getRootFrame(), "Changes...", false, true, Arrays.asList(Button.Close));

        this.project = project;
        this.entity = entity;

        setEditorTitle(project, "Changes related to {0} #" + entity.id, entity.type);

        restService = project.getComponent(RestService.class);
        entityService = project.getComponent(EntityService.class);
        metadataService = project.getComponent(MetadataService.class);

        contentPanel = new JPanel();
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);

        filler = new JPanel();
        filler.setBackground(Color.WHITE);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(new EmptyBorder(10, 10, 10, 10));
        outer.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
        getContentPane().add(outer, BorderLayout.CENTER);

        quickSearch = new QuickSearchPanel("", this, false);
        quickSearch.setBorder(new EmptyBorder(0, 0, 10, 0));
        outer.add(quickSearch, BorderLayout.NORTH);

        getRootPane().setDefaultButton(getButton(Button.Close));

        setSize(800, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                restService.removeServerTypeListener(ChangesDialog.this);
            }
        });
        restService.addServerTypeListener(this);

        executeFilter("");
    }

    @Override
    public void executeFilter(final String value) {
        quickSearch.setRunning(true);
        showMessage("Loading metadata...");
        metadataService.loadEntityMetadataAsync("changeset", new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                if (!value.isEmpty()) {
                    filterQuery(value, metadata);
                } else {
                    noFilterQuery(metadata);
                }
            }

            @Override
            public void metadataFailed() {
                showMessage("Failed to load metadata");
                quickSearch.setRunning(false);
            }
        });
    }

    private void showMessage(String message) {
        map.clear();
        contentPanel.removeAll();
        contentPanel.add(new JLabel(message));
        contentPanel.revalidate();
        repaint();
    }

    private void filterQuery(String value, Metadata metadata) {
        String alias = restService.getModelCustomization().getDevelopmentAlias(entity.type);
        List<EntityQuery> queries = new LinkedList<EntityQuery>();
        if(metadata.getField("description").isCanFilter()) {
            addQuery(queries, alias, "description", value);
        }
        addQuery(queries, alias, "owner", value);
        if(metadata.getField("rev") != null) {
            addQuery(queries, alias, "rev", value);
        }
        addQuery(queries, alias, "changeset-file", "path", value);
        queryChanges(queries, value, metadata);
    }

    private void addQuery(List<EntityQuery> queries, String alias, String property, String filter) {
        EntityQuery query = new EntityQuery("changeset");
        query.getCrossFilter(entity.type, alias).setValue("id", String.valueOf(entity.id));
        query.setValue(property, "'*"+filter+"*'");
        queries.add(query);
    }

    private void addQuery(List<EntityQuery> queries, String alias, String crossEntity, String property, String filter) {
        EntityQuery query = new EntityQuery("changeset");
        query.getCrossFilter(entity.type, alias).setValue("id", String.valueOf(entity.id));
        query.setValue(crossEntity, property, "'*"+filter+"*'");
        queries.add(query);
    }

    private void noFilterQuery(Metadata metadata) {
        String alias = restService.getModelCustomization().getDevelopmentAlias(entity.type);
        EntityQuery query = new EntityQuery("changeset");
        query.getCrossFilter(entity.type, alias).setValue("id", String.valueOf(entity.id));
        queryChanges(Arrays.asList(query), null, metadata);
    }

    private void queryChanges(final List<EntityQuery> queries, final String filter, final Metadata metadata) {
        showMessage("Loading information...");
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                final LinkedHashSet<Entity> changesets = new LinkedHashSet<Entity>();

                try {
                    for(EntityQuery query: queries) {
                        changesets.addAll(entityService.query(query));
                    }
                } catch (Exception e) {
                    // probably not connected
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        @Override
                        public void run() {
                            showMessage("Failed to load data");
                        }
                    });
                    return;
                }

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    public void run() {
                        if(!changesets.isEmpty()) {
                            showChangesets(changesets, metadata);
                        } else {
                            showMessage("No changes match your filter");
                            quickSearch.setRunning(false);
                        }
                    }
                });

                for(final Entity changeset: changesets) {
                    EntityQuery fileQuery = new EntityQuery("changeset-file");
                    fileQuery.setValue("parent-id", changeset.getPropertyValue("id"));
                    final EntityList files = entityService.query(fileQuery);
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        public void run() {
                            showChangesetFiles(changeset.getId(), files, filter);
                            quickSearch.setRunning(false);
                        }
                    });
                }
            }
        });

    }

    private void showChangesets(Collection<Entity> changesets, Metadata metadata) {
        contentPanel.removeAll();
        for (Entity changeset: changesets) {
            ChangesetPanel panel = new ChangesetPanel(project, changeset, metadata.getField("description").isCanFilter());
            map.put(changeset.getId(), panel);
            addChangesetPanel(panel);
        }
        addFiller();
        contentPanel.revalidate();
        repaint();
    }

    private void addChangesetPanel(ChangesetPanel panel) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = contentPanel.getComponentCount();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        contentPanel.add(panel, c);
    }

    private void addFiller() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = contentPanel.getComponentCount();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        contentPanel.add(filler, c);
    }

    private void showChangesetFiles(int id, List<Entity> files, String filter) {
        ChangesetPanel changesetPanel = map.get(id);
        changesetPanel.addFiles(files, filter);
    }

    @Override
    public void connectedTo(ServerType serverType) {
        if(serverType.isConnected()) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    executeFilter(quickSearch.getValue());
                }
            });
        }
    }
}
