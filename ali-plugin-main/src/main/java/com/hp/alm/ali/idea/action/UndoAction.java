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

package com.hp.alm.ali.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

public class UndoAction extends AnAction {
    private UndoManager undoManager;

    public UndoAction(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    public void actionPerformed(AnActionEvent e) {
        if(undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    public static void installUndoRedoSupport(JTextComponent textComponent) {
        final UndoManager undoManager = new UndoManager();
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        new UndoAction(undoManager).registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts("$Undo")), textComponent);
        new RedoAction(undoManager).registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts("$Redo")), textComponent);
        textComponent.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent event) {
                undoManager.addEdit(event.getEdit());
            }
        });
    }
}
