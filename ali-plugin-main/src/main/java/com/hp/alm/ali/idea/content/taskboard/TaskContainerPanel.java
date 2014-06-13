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

import com.hp.alm.ali.idea.ui.WrapLayout;
import com.hp.alm.ali.idea.model.Entity;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

public class TaskContainerPanel extends JPanel {

    private Entity item;
    private String status;
    private BacklogItemPanel backlogItemPanel;
    private TaskDropTargetListener taskDropTargetListener;

    private JComponent dragTarget;

    public TaskContainerPanel(BacklogItemPanel backlogItemPanel, String status, Entity item) {
        super(new WrapLayout(FlowLayout.LEFT));

        this.backlogItemPanel = backlogItemPanel;
        this.status = status;
        this.item = item;

        dragTarget =  new JPanel();
        dragTarget.setVisible(false);
        dragTarget.setBorder(TaskPanel.createPanelBorder());
        add(dragTarget);

        taskDropTargetListener = new TaskDropTargetListener();
        setDropTarget(new DropTarget(this, DnDConstants.ACTION_MOVE, taskDropTargetListener, true, null));
    }

    public void addTask(TaskPanel taskPanel) {
        add(taskPanel, getComponentCount() - 1);
        taskPanel.setDropTargetListener(taskDropTargetListener);
    }

    public Dimension getPreferredSize() {
        return new Dimension(TaskBoardPanel.MIN_COLUMN_WIDTH, super.getPreferredSize().height);
    }

    private class TaskDropTargetListener extends DropTargetAdapter {

        public void drop(DropTargetDropEvent event) {
            try {
                dragTarget.setVisible(false);
                Entity task = (Entity) event.getTransferable().getTransferData(TaskTransferable.dataFlavor);
                backlogItemPanel.moveTask(task, status);
                event.dropComplete(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void dragOver(DropTargetDragEvent event) {
            try {
                Transferable tr = event.getTransferable();
                if (event.isDataFlavorSupported(TaskTransferable.dataFlavor)) {
                    Entity task = (Entity) tr.getTransferData(TaskTransferable.dataFlavor);
                    if(task.getPropertyValue("release-backlog-item-id").equals(item.getPropertyValue("id")) && !status.equals(task.getPropertyValue("status"))) {
                        if(getComponentCount() > 1)  {
                            // assume that inserted task will have the same dimension as the previous
                            dragTarget.setPreferredSize(getComponent(getComponentCount() - 2).getSize());
                        } else {
                            // assume it will have the same size
                            dragTarget.setPreferredSize(backlogItemPanel.getTaskPanel(task).getSize());
                        }
                        dragTarget.setVisible(true);
                        revalidate();
                        repaint();
                        return;
                    }
                }
            } catch (Exception e) {
            }
            event.rejectDrag();
            dragTarget.setVisible(false);
        }

        public void dragExit(DropTargetEvent event) {
            dragTarget.setVisible(false);
        }
    }
}
