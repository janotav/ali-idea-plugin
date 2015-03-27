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

package com.hp.alm.ali.idea.content.devmotive;

import com.hp.alm.ali.idea.action.devmotive.DevMotiveReopenAction;
import com.hp.alm.ali.idea.content.AliContentFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.ActiveAnnotationGutter;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Cursor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DevMotiveAnnotationGutter implements ActiveAnnotationGutter, ChangeListener, ContentManagerListener {

    private Project project;
    private FileAnnotation annotation;
    private DevMotivePanel devMotivePanel;
    private EditorGutterComponentEx editorGutterComponentEx;
    private String unknownLine;

    public DevMotiveAnnotationGutter(Project project, FileAnnotation annotation, EditorGutter editorGutter) {
        this.project = project;
        this.annotation = annotation;

        devMotivePanel = AliContentFactory.addDevMotiveContent(project, annotation.getFile(), this, true);
        devMotivePanel.addChangeListener(this);

        if (editorGutter instanceof EditorGutterComponentEx) {
            editorGutterComponentEx = (EditorGutterComponentEx) editorGutter;
            unknownLine = "???";
        } else {
            // unable to resize, make sure to reserve enough room
            unknownLine = "??????????";
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        updateAnnotations();
    }

    private void updateAnnotations() {
        if (editorGutterComponentEx != null) {
            editorGutterComponentEx.revalidateMarkup();
        }
    }

    @Override
    public void doAction(int line) {
        if (devMotivePanel == null) {
            return;
        }
        VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(line);
        if (revisionNumber != null) {
            devMotivePanel.selectRevision(revisionNumber);
        }
    }

    @Override
    public Cursor getCursor(int line) {
        if (devMotivePanel == null) {
            return Cursor.getDefaultCursor();
        }
        VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(line);
        if (revisionNumber != null) {
            Collection<WorkItem> workItems = devMotivePanel.getWorkItemsByRevisionNumber(revisionNumber, true);
            if (workItems != null && !workItems.isEmpty() ) {
                return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }
        return Cursor.getDefaultCursor();
    }

    @Nullable
    @Override
    public String getLineText(int line, Editor editor) {
        VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(line);
        if (revisionNumber == null) {
            return null;
        }
        if (devMotivePanel == null) {
            return "---";
        }
        Collection<WorkItem> workItems = devMotivePanel.getWorkItemsByRevisionNumber(revisionNumber, false);
        if (workItems == null) {
            return unknownLine;
        }
        StringBuffer buf = new StringBuffer();
        for (WorkItem workItem: workItems) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            switch (workItem.getType()) {
                case DEFECT:
                    buf.append("D: ");
                    break;

                case USER_STORY:
                    buf.append("U: ");
                    break;
            }
            buf.append(workItem.getId());
        }
        return buf.toString();
    }

    @Nullable
    @Override
    public String getToolTip(int line, Editor editor) {
        VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(line);
        if (revisionNumber == null) {
            return null;
        }
        if (devMotivePanel == null) {
            return "Dev Motive closed";
        }
        Collection<WorkItem> workItems = devMotivePanel.getWorkItemsByRevisionNumber(revisionNumber, true);
        if (workItems == null) {
            devMotivePanel.load(revisionNumber);
            return "Loading association information...";
        } else if (!workItems.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            for (WorkItem workItem: workItems) {
                if (buf.length() > 0) {
                    buf.append("\n\n");
                }
                buf.append(getWorkItemToolTip(workItem));
            }
            return buf.toString();
        } else {
            return null;
        }
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
        VcsRevisionNumber revisionNumber = annotation.getLineRevisionNumber(line);
        if (revisionNumber != null && revisionNumber.equals(annotation.getCurrentRevision())) {
            return EditorFontType.BOLD_ITALIC;
        } else {
            return EditorFontType.ITALIC;
        }
    }

    @Nullable
    @Override
    public ColorKey getColor(int line, Editor editor) {
        return null;
    }

    @Nullable
    @Override
    public Color getBgColor(int line, Editor editor) {
        return null;
    }

    @Override
    public List<AnAction> getPopupActions(int line, Editor editor) {
        return Arrays.<AnAction>asList(new DevMotiveReopenAction(project, annotation));
    }

    @Override
    public void gutterClosed() {
        releasePanel();
    }

    private void releasePanel() {
        if (devMotivePanel != null) {
            devMotivePanel.removeChangeListener(this);
            devMotivePanel = null;
        }
    }

    private String getWorkItemToolTip(WorkItem workItem) {
        if (WorkItem.Type.NONE.equals(workItem.getType())) {
            return workItem.getName();
        } else {
            return workItem.getType() + " #" + workItem.getId() + ": " + workItem.getName();
        }
    }

    @Override
    public void contentAdded(ContentManagerEvent event) {
        if (devMotivePanel == null && event.getContent().getComponent() instanceof DevMotivePanel) {
            DevMotivePanel panel = (DevMotivePanel) event.getContent().getComponent();
            if (annotation.getFile().equals(panel.getFile())) {
                devMotivePanel = panel;
                devMotivePanel.addChangeListener(this);
            }
        }
    }

    @Override
    public void contentRemoved(ContentManagerEvent event) {
        if (devMotivePanel != null && devMotivePanel.equals(event.getContent().getComponent())) {
            releasePanel();
            updateAnnotations();
        }
    }

    @Override
    public void contentRemoveQuery(ContentManagerEvent event) {
    }

    @Override
    public void selectionChanged(ContentManagerEvent event) {
    }
}
