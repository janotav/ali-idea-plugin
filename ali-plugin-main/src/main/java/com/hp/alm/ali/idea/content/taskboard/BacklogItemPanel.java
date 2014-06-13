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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.TaskBoardConfiguration;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.hp.alm.ali.idea.ui.EntityLabel;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.ScrollablePanel;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.ui.Highlightable;
import com.hp.alm.ali.idea.ui.SimpleHighlight;
import com.hp.alm.ali.idea.ui.editor.TaskEditor;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BacklogItemPanel extends ScrollablePanel implements Highlightable, DataProvider {

    public static final String ITEM_NEW = "New";
    public static final String ITEM_IN_PROGRESS = "In Progress";
    public static final String ITEM_IN_TESTING = "In Testing";
    public static final String ITEM_DONE = "Done";

    public static final Dimension DIMENSION = new Dimension(300, 160);

    private Project project;
    private EntityLabelService entityLabelService;
    private EntityService entityService;
    private ActiveItemService activeItemService;
    private TaskBoardConfiguration taskBoardConfiguration;
    private AliProjectConfiguration aliProjectConfiguration;
    private Map<String, TaskContainerPanel> taskContainers;
    private JComponent taskContent;
    private Map<Integer, TaskPanel> tasks;
    private Header header;
    private Content content;
    private JTextPane entityName;
    private Entity item;
    private SimpleHighlight simpleHighlight;
    private TaskBoardFilter filter;

    public BacklogItemPanel(Project project, Entity item, TaskBoardFilter filter) {
        super(new BorderLayout());

        this.project = project;
        this.item = item;
        this.filter = filter;
        entityLabelService = project.getComponent(EntityLabelService.class);
        entityService = project.getComponent(EntityService.class);
        activeItemService = project.getComponent(ActiveItemService.class);
        taskBoardConfiguration = project.getComponent(TaskBoardConfiguration.class);
        aliProjectConfiguration = project.getComponent(AliProjectConfiguration.class);

        header = new Header();
        header.setBorder(new EmptyBorder(0, 5, 0, 5));
        add(header, BorderLayout.NORTH);
        entityName = new JTextPane();
        entityName.setBackground(getBackground());
        entityName.setEditable(false);
        add(entityName, BorderLayout.CENTER);
        content = new Content();
        content.setBorder(new EmptyBorder(0, 5, 10, 5));
        add(content, BorderLayout.SOUTH);

        tasks = new HashMap<Integer, TaskPanel>();

        taskContainers = new HashMap<String, TaskContainerPanel>();
        taskContainers.put(TaskPanel.TASK_NEW, new TaskContainerPanel(this, TaskPanel.TASK_NEW, item));
        taskContainers.put(TaskPanel.TASK_IN_PROGRESS, new TaskContainerPanel(this, TaskPanel.TASK_IN_PROGRESS, item));
        taskContainers.put(TaskPanel.TASK_COMPLETED, new TaskContainerPanel(this, TaskPanel.TASK_COMPLETED, item));

        taskContent = new JPanel(new GridLayout(1, 3));
        taskContent.add(getTaskContainer(TaskPanel.TASK_NEW));
        taskContent.add(getTaskContainer(TaskPanel.TASK_IN_PROGRESS));
        taskContent.add(getTaskContainer(TaskPanel.TASK_COMPLETED));

        Color gridColor = UIManager.getDefaults().getColor("Table.gridColor");
        taskContent.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, gridColor));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, gridColor));

        // we don't use gap in grid layout to avoid trailing line (on the right)
        getTaskContainer(TaskPanel.TASK_IN_PROGRESS).setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, gridColor));
        getTaskContainer(TaskPanel.TASK_COMPLETED).setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, gridColor));

        simpleHighlight = new SimpleHighlight(entityName);

        setPreferredSize(DIMENSION);

        update(item);
    }

    public void update(Entity item) {
        this.item = item;

        header.update();
        content.update();
        entityName.setText(item.getPropertyValue("entity-name"));
        applyFilter();
    }

    public void updateTask(final Entity task, final boolean created) {
        TaskPanel taskPanel = tasks.get(task.getId());
        if(taskPanel == null) {
            taskPanel = new TaskPanel(task, project, this);
            tasks.put(task.getId(), taskPanel);
            getTaskContainer(task.getPropertyValue("status")).addTask(taskPanel);
        }
        taskPanel.update(task);
        applyFilter();

        final TaskPanel fTaskPanel = taskPanel;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // task panel is (most of the time) not painted properly when doing full reload (refresh in the Settings dialog)
                // if the task is filtered out, only half of the task rectangle has the correct alpha
                // additional repaint (later) seems to fix the issue
                fTaskPanel.repaint();

                if(created) {
                    TaskContainerPanel taskContainer = (TaskContainerPanel)fTaskPanel.getParent();
                    taskContainer.scrollRectToVisible(fTaskPanel.getBounds());
                }
            }
        });
    }

    public void moveTask(final Entity task, final String newStatus) {
        final TaskPanel taskPanel = tasks.get(task.getId());

        task.setProperty("status", newStatus);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                HashSet<String> properties = new HashSet<String>();
                properties.add("status");
                if (TaskPanel.TASK_IN_PROGRESS.equals(newStatus)) {
                    // when moving task to progress
                    if (taskBoardConfiguration.isAssignTask() && task.getPropertyValue("assigned-to").isEmpty()) {
                        // assign them to me if they are unassigned
                        task.setProperty("assigned-to", aliProjectConfiguration.getUsername());
                        properties.add("assigned-to");
                    }
                    if (taskBoardConfiguration.isActivateItem()) {
                        // activate work item
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            @Override
                            public void run() {
                                Entity workItem = new Entity(item.getPropertyValue("entity-type"), Integer.valueOf(item.getPropertyValue("entity-id")));
                                activeItemService.activate(workItem, true, false);
                            }
                        });
                    }
                }
                final Entity updatedTask = entityService.updateEntity(task, properties, false, true);
                if(updatedTask == null) {
                    return; // update failed
                }
                final Entity updatedRbi;
                if (TaskPanel.TASK_COMPLETED.equals(newStatus)) {
                    String tasksCompletedStatus = taskBoardConfiguration.getTasksCompletedStatus();
                    EntityRef workItem = new EntityRef(item.getPropertyValue("entity-type"), Integer.valueOf(item.getPropertyValue("entity-id")));
                    boolean deactivate = taskBoardConfiguration.isDeactivateItem() &&
                            activeItemService.getActiveItem() != null &&
                            activeItemService.getActiveItem().equals(workItem);
                    if (tasksCompletedStatus != null || deactivate) {
                        // when task moves to completed, we need to see if there are incomplete tasks remaining
                        EntityQuery query = new EntityQuery("project-task");
                        query.addColumn("id", 1);
                        query.setValue("status", "<> Completed");
                        query.setValue("release-backlog-item-id", String.valueOf(item.getId()));
                        query.setPropertyResolved("status", true);
                        EntityList incompleteTasks = entityService.query(query);
                        if(incompleteTasks.isEmpty()) {
                            if (tasksCompletedStatus != null) {
                                // move backlog item to target state
                                item.setProperty("status", tasksCompletedStatus);
                                updatedRbi = entityService.updateEntity(item, Collections.singleton("status"), false, true);
                            } else {
                                updatedRbi = null;
                            }
                            if (deactivate) {
                                // deactivate work item
                                UIUtil.invokeLaterIfNeeded(new Runnable() {
                                    @Override
                                    public void run() {
                                        activeItemService.activate(null, true, false);
                                    }
                                });
                            }
                        } else {
                            updatedRbi = null;
                        }
                    } else {
                        updatedRbi = null;
                    }
                } else if (TaskPanel.TASK_IN_PROGRESS.equals(newStatus)) {
                    // when task moves to progress, backlog item must move to progress too
                    item.setProperty("status", newStatus);
                    updatedRbi = entityService.updateEntity(item, Collections.singleton("status"), false, true);
                } else if(ITEM_DONE.equals(item.getProperty("status"))) {
                    // when task moves to new and backlog item was completed, move it to progress
                    item.setProperty("status", TaskPanel.TASK_IN_PROGRESS);
                    updatedRbi = entityService.updateEntity(item, Collections.singleton("status"), false, true);
                } else {
                    // do nothing otherwise
                    updatedRbi = null;
                }
                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        taskPanel.update(updatedTask);

                        if (updatedRbi != null) {
                            update(updatedRbi);
                        }
                    }
                });
            }
        });
    }

    public TaskContainerPanel getTaskContainer(String status) {
        return taskContainers.get(status);
    }

    private Double getValue(Entity entity, String property) {
        try {
            return Double.valueOf(entity.getPropertyValue(property));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public JComponent getTaskContent() {
        return taskContent;
    }

    public void applyFilter() {
        Map<Entity, Boolean> matches = matches();
        if(matches.containsValue(true)) {
            setHighlight(this, matches.get(item), filter.getFilter());
            for(TaskPanel taskPanel: tasks.values()) {
                Entity task = taskPanel.getTask();
                setHighlight(taskPanel, matches.get(task), filter.getFilter());
            }
            getTaskContent().setVisible(true);
            setVisible(true);
        } else {
            getTaskContent().setVisible(false);
            setVisible(false);
        }
    }

    private void setHighlight(Highlightable panel, boolean match, String filter) {
        panel.setFilter(filter);
        panel.setMatch(match);
    }

    private Map<Entity, Boolean> matches() {
        HashMap<Entity, Boolean> ret = new HashMap<Entity, Boolean>();

        boolean typeMatch;
        if(item.getPropertyValue("entity-type").equals("requirement")) {
            typeMatch = filter.isUserStories();
        } else {
            typeMatch = filter.isDefects();
        }
        if(!filter.getStatus().contains(item.getPropertyValue("status"))) {
            typeMatch = false;
        }
        if (!filter.isBlocked() && !item.getPropertyValue("blocked").isEmpty()) {
            typeMatch = false;
        }

        ret.put(item, typeMatch && filterMatches(filter.getFilter(), item, "entity-name") && ownerMatches(filter.getAssignedTo(), item, "owner"));

        for(TaskPanel taskPanel: tasks.values()) {
            Entity task = taskPanel.getTask();
            ret.put(task, typeMatch && filterMatches(filter.getFilter(), task, "description") && ownerMatches(filter.getAssignedTo(), task, "assigned-to"));
        }

        return ret;
    }

    private boolean filterMatches(String filter, Entity entity, String property) {
        if(filter == null || filter.isEmpty()) {
            return true;
        }

        if(StringUtils.containsIgnoreCase(entity.getPropertyValue(property), filter)) {
            return true;
        }

        return false;
    }

    private boolean ownerMatches(String assignedTo, Entity entity, String property) {
        if(assignedTo == null) {
            return true;
        }

        return assignedTo.equals(entity.getPropertyValue(property));
    }

    @Override
    public void setFilter(String filter) {
        simpleHighlight.setFilter(filter);
    }

    @Override
    public void setMatch(boolean match) {
    }

    public TaskPanel getTaskPanel(Entity task) {
        return getTaskPanel(task.getId());
    }

    public TaskPanel getTaskPanel(int id) {
        return tasks.get(id);
    }

    public void retainTasks(Set<Integer> taskIds) {
        boolean modified = false;
        for(Iterator<Integer> it = tasks.keySet().iterator(); it.hasNext(); ) {
            int taskId = it.next();
            if(!taskIds.contains(taskId)) {
                TaskPanel taskPanel = tasks.get(taskId);
                TaskContainerPanel taskContainer = getTaskContainer(taskPanel.getStatus());
                taskContainer.remove(taskPanel);
                taskContainer.repaint();
                it.remove();
                modified = true;
            }
        }
        if(modified) {
            applyFilter();
        }
    }

    public void removeTaskPanel(TaskPanel taskPanel) {
        TaskContainerPanel taskContainer = getTaskContainer(taskPanel.getStatus());
        taskContainer.remove(taskPanel);
        taskContainer.repaint();
        tasks.remove(taskPanel.getTask().getId());
        applyFilter();
    }

    public Entity getItem() {
        return item;
    }

    @Override
    public Object getData(@NonNls String s) {
        if("entity-list".equals(s)) {
            Entity entity = new Entity(item.getPropertyValue("entity-type"), Integer.valueOf(item.getPropertyValue("entity-id")));
            entity.mergeRelatedEntity(item);
            return Arrays.asList(entity);
        }
        return null;
    }

    private class RbiStatus extends JPanel {

        private JLabel value;

        public RbiStatus() {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);

            add(new JLabel("Status: "));
            value = new JLabel();
            add(value);
        }

        public void setValue(String status) {
            value.setText(status);
        }
    }

    private class Content extends JPanel {

        private Gauge gauge;
        private RbiStatus status;
        private JLabel remainingLabel;

        public Content() {
            super(new BorderLayout());

            setOpaque(false);

            JPanel north = new JPanel(new BorderLayout());
            north.setOpaque(false);
            gauge = new Gauge(0);
            north.add(gauge, BorderLayout.CENTER);
            add(north, BorderLayout.NORTH);

            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            status = new RbiStatus();
            south.add(status, BorderLayout.WEST);
            remainingLabel = new JLabel();
            remainingLabel.setBorder(new EmptyBorder(0, 0, 0, 3));
            south.add(remainingLabel, BorderLayout.EAST);
            add(south, BorderLayout.SOUTH);

            update();
        }

        public void update() {
            Double estimated = getValue(item, "estimated");
            Double remaining = getValue(item, "remaining");
            if(estimated > 0.0) {
                gauge.setValue((estimated - remaining) / estimated);
            } else {
                gauge.setValue(0);
            }
            status.setValue(item.getPropertyValue("status"));
            if(remaining > 0.0) {
                remainingLabel.setText(Math.round(remaining) + " hours remaining");
                remainingLabel.setVisible(true);
            } else {
                remainingLabel.setVisible(false);
            }
        }
    }

    private class Header extends JPanel {

        private JLabel blocked;

        public Header() {
            super(new BorderLayout());

            setOpaque(false);

            JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            blocked = new JLabel(IconLoader.getIcon("/blocked.png"));
            blocked.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
            westPanel.add(blocked);
            westPanel.add(new EntityLabel(item.getPropertyValue("entity-type"), entityLabelService, " | ID " + item.getPropertyValue("entity-id")));
            add(westPanel, BorderLayout.WEST);

            JPanel toolbar = new JPanel();
            toolbar.setOpaque(false);
            LinkLabel addLink = new LinkLabel("", IconLoader.getIcon("/general/add.png"));
            addLink.setToolTipText("Add Task");
            addLink.setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel linkLabel, Object o) {
                    TaskEditor editor = new TaskEditor(project, item.getId());
                    editor.execute();
                }
            }, null);
            toolbar.add(addLink);
            final LinkLabel moreLink = new LinkLabel("", IconLoader.getIcon("/actions/forward.png"));
            moreLink.setToolTipText("More Actions");
            moreLink.setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel linkLabel, Object o) {
                    ActionPopupMenu actionPopup = ActionUtil.createEntityActionPopup("taskboard");
                    actionPopup.getComponent().show(moreLink, 0, 0);
                }
            }, null);
            toolbar.add(moreLink);
            add(toolbar, BorderLayout.EAST);

            update();
        }

        public void update() {
            String reason = item.getPropertyValue("blocked");
            if (!reason.isEmpty()) {
                blocked.setToolTipText("Blocking reason: " + reason);
                blocked.setVisible(true);
            } else {
                blocked.setVisible(false);
            }
        }
    }
}
