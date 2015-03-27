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

package com.hp.alm.ali.idea.action.devmotive;

import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.content.devmotive.DevMotiveAnnotationGutter;
import com.hp.alm.ali.idea.content.devmotive.DevMotivePanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.ActiveAnnotationGutter;
import com.intellij.openapi.vcs.actions.AnnotationGutterLineConvertorProxy;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;

public class DevMotiveGutterAction extends AnAction {

    private FileAnnotation annotation;

    public DevMotiveGutterAction(FileAnnotation annotation) {
        super("DevMotive");
        this.annotation = annotation;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = getEventProject(e);
        if (project != null) {
            registerDevMotiveTextAnnotation(project, annotation);
            getTemplatePresentation().setEnabled(false);
        }
    }

    public static void registerDevMotiveTextAnnotation(Project project, FileAnnotation annotation) {
        DevMotivePanel devMotivePanel = AliContentFactory.addDevMotiveContent(project, annotation.getFile(), true);

        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, annotation.getFile());
        final Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        ActiveAnnotationGutter devMotiveGutterProvider = new DevMotiveAnnotationGutter(project, annotation, devMotivePanel, editor.getGutter());

        UpToDateLineNumberProvider getUpToDateLineNumber = new UpToDateLineNumberProviderImpl(editor.getDocument(), project);
        AnnotationGutterLineConvertorProxy proxy = new AnnotationGutterLineConvertorProxy(getUpToDateLineNumber, devMotiveGutterProvider);
        editor.getGutter().registerTextAnnotation(proxy, proxy);
    }
}
