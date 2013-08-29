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

import com.hp.alm.ali.idea.model.Entity;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;

public class TaskTransferable extends DragSourceAdapter implements Transferable, DragGestureListener {

    public static DataFlavor dataFlavor = new DataFlavor(Entity.class, Entity.class.getSimpleName());

    private TaskPanel taskPanel;

    public TaskTransferable(TaskPanel taskPanel) {
        this.taskPanel = taskPanel;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        try {
           DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultMoveDrop, this, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{ dataFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return dataFlavor.equals(flavor);
    }

    @Override
    public Entity getTransferData(DataFlavor flavor) {
        return taskPanel.getTask();
    }
}
