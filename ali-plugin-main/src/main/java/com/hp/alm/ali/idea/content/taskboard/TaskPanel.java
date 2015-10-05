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

import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.model.type.UserType;
import com.hp.alm.ali.idea.ui.editor.TaskAddInvestedEditor;
import com.hp.alm.ali.idea.ui.event.PopupAdapter;
import com.hp.alm.ali.idea.ui.Highlightable;
import com.hp.alm.ali.idea.ui.SimpleHighlight;
import com.hp.alm.ali.idea.ui.editor.TaskEditor;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NonNls;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TaskPanel extends JLayeredPane implements Highlightable, DataProvider {

    public static final Dimension SIZE = new Dimension(200, 120);

    public static final String TASK_IN_PROGRESS = "In Progress";
    public static final String TASK_NEW = "New";
    public static final String TASK_COMPLETED = "Completed";

    private Entity task;
    private JPanel statusPanel;
    private JLabel remainingLabel;
    private JLabel assigned;
    private TimePanel timePanel;
    private JPanel content;
    private RestrictedTextPane description;
    private boolean matched;

    private Set<Object> forcedMatch = Collections.synchronizedSet(new HashSet<Object>());

    private Gauge gauge;
    private JPanel overlay;
    private JPanel handlePanel;
    private JPanel panelForBorder;
    private SimpleHighlight simpleHighlight;
    private Project project;
    private BacklogItemPanel backlogItemPanel;
    private TranslateService translateService;
    private Translator userTranslator;

    public TaskPanel(final Entity task, final Project project, BacklogItemPanel pBacklogItemPanel) {
        this.task = task;
        this.project = project;
        this.backlogItemPanel = pBacklogItemPanel;
        this.translateService = project.getComponent(TranslateService.class);
        this.userTranslator = project.getComponent(UserType.class).getTranslator();

        content = new JPanel(new BorderLayout());
        Color bg = content.getBackground();
        boolean darkTheme = isDarkTheme(bg);
        if (!darkTheme) {
            content.setBackground(new Color(0xFd, 0xF8, 0xCE));
        }
        content.setBorder(new EmptyBorder(2, 12, 2, 2)); // leave room for the handle
        add(content, JLayeredPane.DEFAULT_LAYER);

        description = new RestrictedTextPane(SIZE.width - 30, 45);
        content.add(description, BorderLayout.CENTER);

        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        JPanel assignedAndTime = new JPanel(new BorderLayout());
        assignedAndTime.setOpaque(false);

        assigned = new JLabel();
        assignedAndTime.add(assigned, BorderLayout.WEST);

        timePanel = new TimePanel(project, this);
        assignedAndTime.add(timePanel, BorderLayout.EAST);
        statusPanel.add(assignedAndTime, BorderLayout.SOUTH);
        gauge = new Gauge(0);
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(gauge, BorderLayout.CENTER);
        remainingLabel = new JLabel(IconLoader.getIcon("/general/secondaryGroup.png"));
        north.add(remainingLabel, BorderLayout.WEST);
        statusPanel.add(north, BorderLayout.NORTH);
        content.add(statusPanel, BorderLayout.SOUTH);
        // no mouse events due to overlays, must be handled in the upper layer
        final JLabel moreLink = new JLabel(IconLoader.getIcon("/actions/forward.png"));
        moreLink.setVerticalAlignment(SwingConstants.TOP);
        moreLink.setToolTipText("More Actions");
        content.add(moreLink, BorderLayout.EAST);

        overlay = new JPanel();
        overlay.setOpaque(true);
        if (darkTheme) {
            overlay.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 128));
        } else {
            overlay.setBackground(new Color(0xFF, 0xFF, 0xFF, 128));
        }
        overlay.setVisible(false);
        add(overlay, JLayeredPane.PALETTE_LAYER);

        panelForBorder = new JPanel();
        panelForBorder.setOpaque(false);
        panelForBorder.setBorder(createPanelBorder());
        add(panelForBorder, new Integer(PALETTE_LAYER - 1));

        handlePanel = new JPanel(new BorderLayout());
        handlePanel.setOpaque(false);
        JLabel handle = new JLabel(IconLoader.getIcon("/handle.png"), JLabel.LEADING);
        handle.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        handle.addMouseListener(new PopupAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    TaskEditor taskEditor = new TaskEditor(project, getTask());
                    taskEditor.execute();
                } else {
                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), moreLink);
                    if(moreLink.contains(point)) {
                        ActionPopupMenu popupMenu = ActionUtil.createActionPopup("hpali.task", "taskboard");
                        popupMenu.getComponent().show(moreLink, 0, 0);
                    }
                    point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), timePanel.effortLabel);
                    if (timePanel.effortLabel.contains(point)) {
                        TaskAddInvestedEditor taskAddInvestedEditor = new TaskAddInvestedEditor(project, task);
                        taskAddInvestedEditor.execute();
                    }
                }
            }
            @Override
            public void onPopup(MouseEvent e) {
                ActionPopupMenu popupMenu = ActionUtil.createActionPopup("hpali.task", "taskboard");
                popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
        });
        handlePanel.add(handle, BorderLayout.CENTER);
        add(handlePanel, JLayeredPane.DRAG_LAYER);

        simpleHighlight = new SimpleHighlight(description);

        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(handle, DnDConstants.ACTION_COPY, new TaskTransferable(this));
    }

    public static Border createPanelBorder() {
        return BorderFactory.createLineBorder(UIManager.getDefaults().getColor("Table.gridColor"));
    }

    public Dimension getPreferredSize() {
        // NOTE: the dimension of the overlay is 1 pixel less otherwise the base layer is not repainted sometimes (e.g. when scrolling)
        content.setBounds(new Rectangle(0, 0, SIZE.width, SIZE.height));
        overlay.setBounds(new Rectangle(0, 0, SIZE.width - 1, SIZE.height - 1));
        handlePanel.setBounds(new Rectangle(0, 0, SIZE.width - 1, SIZE.height - 1));
        panelForBorder.setBounds(new Rectangle(0, 0, SIZE.width, SIZE.height));
        return SIZE;
    }

    public void update(Entity task) {
        this.task = task;

        moveTaskAsNeeded();

        String remaining = task.getPropertyValue("remaining");
        String invested = task.getPropertyValue("invested");
        String status = task.getPropertyValue("status");
        String assignedTo = task.getPropertyValue("assigned-to");

        if(!assignedTo.isEmpty()) {
            translateService.translateAsync(userTranslator, assignedTo, true, new ValueCallback() {
                @Override
                public void value(String value) {
                    assigned.setText(value);
                }
            });
        } else {
            assigned.setText("<Unassigned>");
        }
        if(TASK_IN_PROGRESS.equals(status)) {
            gauge.setVisible(true);
            gauge.setValue(Double.valueOf(invested) / (Double.valueOf(remaining) + Double.valueOf(invested)));
            remainingLabel.setVisible(false);
        } else if(TASK_NEW.equals(status)) {
            gauge.setVisible(false);
            remainingLabel.setVisible(true);
            remainingLabel.setText(remaining + " hours remaining");
        } else {
            gauge.setVisible(false);
            remainingLabel.setVisible(false);
        }

        description.setText(task.getPropertyValue("description"));
        timePanel.update(task);

        getBacklogItemPanel().applyFilter();
    }

    private void moveTaskAsNeeded() {
        TaskContainerPanel oldContainer = (TaskContainerPanel)getParent();
        TaskContainerPanel newContainer = backlogItemPanel.getTaskContainer(task.getPropertyValue("status"));
        if(oldContainer.equals(newContainer)) {
            return;
        }
        newContainer.addTask(this);
        newContainer.revalidate();
        newContainer.repaint();
        oldContainer.revalidate();
        oldContainer.repaint();
    }

    public String getStatus() {
        return task.getPropertyValue("status");
    }

    public Entity getTask() {
        return task;
    }

    @Override
    public void setFilter(String filter) {
        simpleHighlight.setFilter(filter);
    }

    @Override
    public void setMatch(boolean match) {
        matched  = match;
        doOverlay();
    }

    public void addForcedMatch(Object o) {
        forcedMatch.add(o);
        doOverlay();
    }

    public void removeForcedMatch(Object o) {
        forcedMatch.remove(o);
        doOverlay();
    }

    private void doOverlay() {
        overlay.setVisible(!matched && forcedMatch.isEmpty());
        repaint(); // otherwise unmatched background is not painted properly
    }

    private boolean isDarkTheme(Color background) {
        double value = 0.21 * background.getRed() + 0.72 * background.getGreen() + 0.07 * background.getBlue();
        return value < 128;
    }

    public BacklogItemPanel getBacklogItemPanel() {
        return backlogItemPanel;
    }

    @Override
    public Object getData(@NonNls String s) {
        if("entity-list".equals(s)) {
            return Arrays.asList(task);
        }
        return null;
    }

    public void setDropTargetListener(DropTargetListener dropTargetListener) {
        setDropTarget(new DropTarget(this, DnDConstants.ACTION_MOVE, dropTargetListener, true, null));
        description.setDropTarget(new DropTarget(description, DnDConstants.ACTION_MOVE, dropTargetListener, true, null));
    }
}
