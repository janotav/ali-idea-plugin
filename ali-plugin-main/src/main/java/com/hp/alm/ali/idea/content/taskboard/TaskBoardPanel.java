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

package com.hp.alm.ali.idea.content.taskboard;

import com.hp.alm.ali.idea.cfg.TaskBoardConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.queue.QueryQueue;
import com.hp.alm.ali.idea.entity.queue.QueryTarget;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.filter.MultipleItemsChooserFactory;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.QuickSearchPanel;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.ui.ReleaseChooser;
import com.hp.alm.ali.idea.ui.SprintChooser;
import com.hp.alm.ali.idea.services.SprintService;
import com.hp.alm.ali.idea.ui.entity.EntityStatusPanel;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.BoldLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TaskBoardPanel extends JPanel implements SprintService.Listener, EntityListener, QueryTarget {

    public static int MIN_COLUMN_WIDTH = 250;
    public static final String PLACE = "HPALI.TaskBoard";

    private static final List<String> allItemStatuses = Arrays.asList(
            BacklogItemPanel.ITEM_NEW,
            BacklogItemPanel.ITEM_IN_PROGRESS,
            BacklogItemPanel.ITEM_IN_TESTING,
            BacklogItemPanel.ITEM_DONE
    );

    private Project project;
    private EntityService entityService;
    private SprintService sprintService;
    private QueryQueue queue;

    private EntityStatusPanel status;

    private Header header;
    private Content content;
    private TaskPanel forcedTaskPanel;

    public TaskBoardPanel(final Project project) {
        super(new BorderLayout());

        this.project = project;

        status = new EntityStatusPanel(project);
        queue = new QueryQueue(project, status, false, this);

        entityService = project.getComponent(EntityService.class);
        entityService.addEntityListener(this);
        sprintService = project.getComponent(SprintService.class);
        sprintService.addListener(this);

        loadTasks();

        header = new Header();

        content = new Content();
        add(content, BorderLayout.NORTH);

        header.assignedTo.reload();

        // force mouse-over task as visible (otherwise events are captured by the overlay and repaint quirks)
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if(isShowing() && event.getID() == MouseEvent.MOUSE_MOVED) {
                    MouseEvent m = (MouseEvent) event;
                    TaskPanel currentPanel = locateContainer(m, TaskPanel.class);
                    if(currentPanel != null) {
                        if(forcedTaskPanel == currentPanel) {
                            return;
                        } else if(forcedTaskPanel != null) {
                            forcedTaskPanel.removeForcedMatch(this);
                        }
                        forcedTaskPanel = currentPanel;
                        forcedTaskPanel.addForcedMatch(this);
                    } else if(forcedTaskPanel != null) {
                        forcedTaskPanel.removeForcedMatch(this);
                        forcedTaskPanel = null;
                    }
                }
            }
        }, AWTEvent.MOUSE_MOTION_EVENT_MASK);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if(isShowing()) {
                    MouseEvent m = (MouseEvent) event;
                    switch (event.getID()) {
                        case MouseEvent.MOUSE_PRESSED:
                        case MouseEvent.MOUSE_RELEASED:
                            // implement backlog item popup
                            if(m.isPopupTrigger()) {
                                final BacklogItemPanel itemPanel = locateContainer(m, BacklogItemPanel.class);
                                if(itemPanel != null) {
                                    ActionPopupMenu popupMenu = ActionUtil.createEntityActionPopup("taskboard");
                                    Point p = SwingUtilities.convertPoint(m.getComponent(), m.getPoint(), itemPanel);
                                    popupMenu.getComponent().show(itemPanel, p.x, p.y);
                                }
                            }
                            break;

                        case MouseEvent.MOUSE_CLICKED:
                            // implement backlog item double click
                            if(m.getClickCount() > 1) {
                                BacklogItemPanel itemPanel = locateContainer(m, BacklogItemPanel.class);
                                if(itemPanel != null) {
                                    Entity backlogItem = itemPanel.getItem();
                                    Entity workItem = new Entity(backlogItem.getPropertyValue("entity-type"), Integer.valueOf(backlogItem.getPropertyValue("entity-id")));
                                    AliContentFactory.loadDetail(project, workItem, true, true);
                                }
                            }
                    }
                }

            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private <T extends Component> T locateContainer(MouseEvent event, Class<T> clazz) {
        Point p = SwingUtilities.convertPoint((Component) event.getSource(), event.getPoint(), TaskBoardPanel.this);
        Component comp = SwingUtilities.getDeepestComponentAt(this, p.x, p.y);
        return (T) SwingUtilities.getAncestorOfClass(clazz, comp);
    }

    private void loadTasks() {
        Entity sprint = sprintService.getSprint();
        Entity team = sprintService.getTeam();
        if(sprint != null && team != null) {
            loadTasks(sprint, team);
        }
    }

    private void loadTasks(final Entity sprint, final Entity team) {
        EntityQuery query = new EntityQuery("release-backlog-item");
        query.setValue("is-leaf", "Y") ;
        query.setValue("team-id", team.getPropertyValue("id"));
        query.setValue("sprint-id", String.valueOf(sprint.getId()));
        query.setPropertyResolved("is-leaf", true);
        query.setPropertyResolved("team-id", true);
        query.setOrder(new LinkedHashMap<String, SortOrder>(Collections.singletonMap("rank", SortOrder.ASCENDING)));
        queue.query(query);
    }

    private void updateBacklogItems(final EntityList items) {
        final Runnable redo = new Runnable() {
            @Override
            public void run() {
                loadTasks();
            }
        };
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                content.retain(items);
                if(items.isEmpty()) {
                    EntityList empty = EntityList.empty();
                    content.retainTasks(empty);
                    status.loaded(items, redo);
                } else {
                    status.info("Loaded " + EntityStatusPanel.getItemCountString(items, "backlog items") + ", loading tasks...", null, redo);
                    for(int i = 0; i < items.size(); i++) {
                        updateBacklogItem(items.get(i), false, i);
                    }
                }
            }
        });
        if(!items.isEmpty()) {
            EntityQuery query = new EntityQuery("project-task");
            query.setOrValues("release-backlog-item-id", items.getIdStrings());
            final EntityList tasks = entityService.query(query);
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    content.retainTasks(tasks);

                    for (Entity task : tasks) {
                        updateTask(task, false);
                    }
                    status.info("Loaded " + EntityStatusPanel.getItemCountString(items, "backlog items") + " and " + EntityStatusPanel.getItemCountString(tasks, "tasks"), null, redo);
                }
            });
        }
    }

    private void loadTasksOfNewlyAddedBli(Entity entity) {
        EntityQuery query = new EntityQuery("project-task");
        query.setValue("release-backlog-item-id", entity.getPropertyValue("id"));
        final EntityList tasks = entityService.query(query);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                for (Entity task : tasks) {
                    updateTask(task, false);
                }
            }
        });
    }

    private void updateTask(Entity task, boolean created) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        content.updateTask(task, created);
    }

    private void removeTask(int id) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        content.removeTask(id);
    }

    private boolean updateBacklogItem(Entity item, boolean validate, int index) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        return content.updateItem(item, validate, index);
    }

    @Override
    public void onReleaseSelected(Entity release) {
    }

    @Override
    public void onSprintSelected(Entity sprint) {
        loadTasks();
    }

    @Override
    public void onTeamSelected(Entity team) {
        loadTasks();
    }

    public Component getStatusComponent() {
        return status;
    }

    public JComponent getHeader() {
        return header;
    }

    @Override
    public void entityLoaded(final Entity entity, final Event event) {
        if("project-task".equals(entity.getType())) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    updateTask(entity, event == Event.CREATE);
                }
            });
        } else if("release-backlog-item".equals(entity.getType())) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    if(inThisSprint(entity)) {
                        int index = content.getItemIndex(entity);
                        if(updateBacklogItem(entity, true, index) && event != Event.CREATE) {
                            // only load tasks for non-created BLIs only - events will follow for those we create ourselves
                            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadTasksOfNewlyAddedBli(entity);
                                }
                            });
                        }
                    } else {
                        content.removeBacklogItem(entity, true);
                    }
                }
            });
        }
    }

    @Override
    public void entityNotFound(final EntityRef ref, boolean removed) {
        if("project-task".equals(ref.type)) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    removeTask(ref.id);
                }
            });
        } else if("defect".equals(ref.type) || "requirement".equals(ref.type)) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    content.removeBacklogItemOfWorkItem(ref);
                }
            });
        }
    }

    private boolean inThisSprint(Entity entity) {
        Entity sprint = sprintService.getSprint();
        Entity team = sprintService.getTeam();
        return sprint != null && sprint.getPropertyValue("id").equals(entity.getPropertyValue("sprint-id")) &&
                team != null && team.getPropertyValue("id").equals(entity.getPropertyValue("team-id"));
    }

    @Override
    public void handleResult(EntityList list) {
        updateBacklogItems(list);
    }

    private class Header extends JPanel implements TaskBoardFilter {

        private QuickSearchPanel quickSearchPanel;
        private AssignedToComboBox assignedTo;
        private JCheckBox stories;
        private JCheckBox defects;
        private JCheckBox blocked;
        private LinkLabel statusFilter;

        public Header() {
            super(new GridBagLayout());

            final TaskBoardConfiguration conf = project.getComponent(TaskBoardConfiguration.class);
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // using vertical spacing makes the bottom gap too wide (not sure why)
            toolbar.setBorder(BorderFactory.createEtchedBorder());
            toolbar.add(new ReleaseChooser(project));
            quickSearchPanel = new QuickSearchPanel(conf.getFilter(), new QuickSearchPanel.Target() {
                @Override
                public void executeFilter(String value) {
                    conf.setFilter(value);
                    content.applyFilter();
                }
            }, true);
            quickSearchPanel.setBorder(new EmptyBorder(2, 2, 2, 0)); // ad-hoc fix to achieve same layout backlog content has
            toolbar.add(quickSearchPanel);
            SprintChooser sprintChooser = new SprintChooser(project);
            toolbar.add(sprintChooser);
            assignedTo = new AssignedToComboBox(project, conf.getAssignedTo());
            assignedTo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        conf.setAssignedTo(((ComboItem) assignedTo.getSelectedItem()).getKey().toString());
                        content.applyFilter();
                    }
                }
            });
            toolbar.add(new JLabel("Assigned To:"));
            toolbar.add(assignedTo);

            toolbar.add(new JLabel("Status:"));
            statusFilter = new LinkLabel(conf.getShowStatuses(), null);
            statusFilter.setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel aSource, Object aLinkData) {
                    String value;
                    if (TaskBoardConfiguration.ALL_STATUSES.equals(conf.getShowStatuses())) {
                        value = StringUtils.join(allItemStatuses, ";");
                    } else {
                        value = conf.getShowStatuses();
                    }
                    FilterChooser chooser = new MultipleItemsChooserFactory(project, "Status", true, new ItemsProvider.Loader<ComboItem>() {
                        @Override
                        public List<ComboItem> load() {
                            return FilterManager.asItems(allItemStatuses, true, true);
                        }
                    }).createChooser(value);
                    chooser.show();
                    String selectedValue = chooser.getSelectedValue();
                    if (selectedValue != null) {
                        if (selectedValue.isEmpty())  {
                            // treat no selected item as no filter at all
                            conf.setShowStatuses(TaskBoardConfiguration.ALL_STATUSES);
                        } else {
                            List<String> states = Arrays.asList(selectedValue.split(";"));
                            if (states.size() == allItemStatuses.size()) {
                                conf.setShowStatuses(TaskBoardConfiguration.ALL_STATUSES);
                            } else {
                                conf.setShowStatuses(selectedValue);
                            }
                        }
                        statusFilter.setText(conf.getShowStatuses());
                        content.applyFilter();
                    }
                }
            }, null);
            toolbar.add(statusFilter);

            toolbar.add(new JLabel("Show:"));
            stories = new JCheckBox("User stories", conf.isShowUserStories());
            stories.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    conf.setShowUserStories(stories.isSelected());
                    content.applyFilter();
                }
            });
            toolbar.add(stories);
            defects = new JCheckBox("Defects", conf.isShowDefects());
            defects.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    conf.setShowDefects(defects.isSelected());
                    content.applyFilter();
                }
            });
            toolbar.add(defects);
            blocked = new JCheckBox("Blocked", conf.isShowBlocked());
            blocked.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    conf.setShowBlocked(blocked.isSelected());
                    content.applyFilter();
                }
            });
            toolbar.add(blocked);

            JPanel toolbarAndWarning = new JPanel(new BorderLayout());
            toolbarAndWarning.add(toolbar, BorderLayout.NORTH);
            toolbarAndWarning.add(sprintChooser.getWarningPanel());

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 2;
            add(toolbarAndWarning, c);
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 1;
            c.weightx = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            JComponent rbiHeader = columnHeader("Backlog Item");
            rbiHeader.setPreferredSize(new Dimension(BacklogItemPanel.DIMENSION.width, 26));
            add(rbiHeader, c);
            c.fill = GridBagConstraints.BOTH;
            c.gridx++;
            c.weightx = 1;
            JPanel taskHeader = new JPanel(new GridLayout(1, 3));
            taskHeader.add(columnHeader(TaskPanel.TASK_NEW));
            taskHeader.add(columnHeader(TaskPanel.TASK_IN_PROGRESS));
            taskHeader.add(columnHeader(TaskPanel.TASK_COMPLETED));
            add(taskHeader, c);
        }

        @Override
        public String getFilter() {
            return quickSearchPanel.getValue();
        }

        @Override
        public String getAssignedTo() {
            Object selectedItem = assignedTo.getSelectedItem();
            if(selectedItem == null || AssignedToComboBox.ALL.equals(selectedItem)) {
                return null;
            } else if(AssignedToComboBox.UNASSIGNED.equals(selectedItem)) {
                return "";
            } else {
                return ((ComboItem)selectedItem).getKey().toString();
            }
        }

        @Override
        public boolean isUserStories() {
            return stories.isSelected();
        }

        @Override
        public boolean isDefects() {
            return defects.isSelected();
        }

        @Override
        public boolean isBlocked() {
            return blocked.isSelected();
        }

        @Override
        public List<String> getStatus() {
            if (TaskBoardConfiguration.ALL_STATUSES.equals(statusFilter.getText())) {
                return allItemStatuses;
            } else {
                return Arrays.asList(statusFilter.getText().split(";"));
            }
        }
    }

    private class Content extends JPanel {

        private Map<Entity, BacklogItemPanel> items = new HashMap<Entity, BacklogItemPanel>();

        public Content() {
            super(new GridBagLayout());
        }

        public boolean updateItem(Entity item, boolean validate, int index) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = index;
            c.gridx = 0;
            c.fill = GridBagConstraints.VERTICAL;
            c.anchor = GridBagConstraints.NORTHEAST;
            c.insets = new Insets(0, 0, 1, 1);
            BacklogItemPanel backlogItemPanel = items.get(item);
            boolean created;
            if(backlogItemPanel == null) {
                backlogItemPanel = new BacklogItemPanel(project, item, header);
                items.put(item, backlogItemPanel);
                backlogItemPanel.applyFilter();
                if(validate) {
                    revalidate();
                    repaint();

                    final BacklogItemPanel newItemPanel = backlogItemPanel;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            scrollRectToVisible(newItemPanel.getBounds());
                        }
                    });
                }
                created = true;
            } else {
                backlogItemPanel.update(item);
                created = false;
            }
            add(backlogItemPanel, c);

            c.gridx++;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 1, 0);
            add(backlogItemPanel.getTaskContent(), c);

            return created;
        }

        public int getItemIndex(Entity item) {
            BacklogItemPanel backlogItemPanel = items.get(item);
            if (backlogItemPanel != null) {
                return ((GridBagLayout) getLayout()).getConstraints(backlogItemPanel).gridy;
            } else {
                return items.size();
            }
        }

        public void updateTask(Entity task, boolean created) {
            int itemId = Integer.valueOf(task.getPropertyValue("release-backlog-item-id"));
            BacklogItemPanel backlogItemPanel = items.get(new Entity("release-backlog-item", itemId));
            if(backlogItemPanel != null) {
                backlogItemPanel.updateTask(task, created);
            }
        }

        public void removeTask(int id) {
            for(final BacklogItemPanel backlogItemPanel: items.values()) {
                TaskPanel taskPanel = backlogItemPanel.getTaskPanel(id);
                if(taskPanel != null) {
                    backlogItemPanel.removeTaskPanel(taskPanel);
                }
            }
        }

        public void removeBacklogItem(Entity backlogItem, boolean validate) {
            BacklogItemPanel backlogItemPanel = items.remove(backlogItem);
            if(backlogItemPanel != null) {
                GridBagLayout gridBagLayout = (GridBagLayout) getLayout();
                GridBagConstraints c = gridBagLayout.getConstraints(backlogItemPanel);
                remove(backlogItemPanel);
                remove(backlogItemPanel.getTaskContent());
                // shift all items bellow us one row up
                for (BacklogItemPanel item: items.values()) {
                    GridBagConstraints cc = gridBagLayout.getConstraints(item);
                    if(cc.gridy-- > c.gridy) {
                        gridBagLayout.setConstraints(item, cc);
                        cc = gridBagLayout.getConstraints(item.getTaskContent());
                        cc.gridy--;
                        gridBagLayout.setConstraints(item.getTaskContent(), cc);
                    }
                }
                if(validate) {
                    revalidate();
                    repaint();
                }
            }
        }

        public void removeBacklogItemOfWorkItem(EntityRef entity) {
            for(Entity item: items.keySet()) {
                if(new EntityRef(item.getPropertyValue("entity-type"), Integer.valueOf(item.getPropertyValue("entity-id"))).equals(entity)) {
                    removeBacklogItem(item, true);
                    break;
                }
            }
        }

        public void retain(EntityList blis) {
            for(Entity bli: new LinkedList<Entity>(items.keySet())) {
                if(!blis.contains(bli)) {
                    removeBacklogItem(bli, false);
                }
            }
        }

        public void retainTasks(EntityList tasks) {
            HashSet<Integer> ids = new HashSet<Integer>(tasks.getIds());
            for(BacklogItemPanel bliPanel: items.values()) {
                bliPanel.retainTasks(ids);
            }
        }

        public void applyFilter() {
            for(BacklogItemPanel rbiPanel: items.values()) {
                rbiPanel.applyFilter();
            }
        }
    }

    private JComponent columnHeader(String name) {
        BoldLabel label = new BoldLabel(name);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 1, 0, 0),
                BorderFactory.createEtchedBorder()));
        label.setPreferredSize(new Dimension(MIN_COLUMN_WIDTH, 26));
        return label;
    }

}
