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

import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.content.devmotive.DevMotiveAnnotationGutter;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.ActiveAnnotationGutter;
import com.intellij.openapi.vcs.actions.AnnotationGutterLineConvertorProxy;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import com.intellij.util.containers.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DevMotiveGutterAction extends AnAction {

    private FileAnnotation annotation;
    private List<VcsFileRevision> revisions;

    public DevMotiveGutterAction(FileAnnotation annotation) {
        super("Dev Motive");
        this.annotation = annotation;

        AliConfiguration aliConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
        if (aliConfiguration.devMotiveAnnotation) {
            List<VcsFileRevision> revisions = annotation.getRevisions();
            if (revisions != null) {
                Map<VcsRevisionNumber, VcsFileRevision> map = new HashMap<VcsRevisionNumber, VcsFileRevision>();
                for (VcsFileRevision revision : revisions) {
                    map.put(revision.getRevisionNumber(), revision);
                }

                this.revisions = new ArrayList<VcsFileRevision>(annotation.getLineCount());
                for (int i = 0; i < annotation.getLineCount(); i++) {
                    this.revisions.add(map.get(annotation.getLineRevisionNumber(i)));
                }
            }
        } else {
            getTemplatePresentation().setVisible(false);
            getTemplatePresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = getEventProject(e);
        if (project != null) {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                ActiveAnnotationGutter devMotiveGutterProvider = new DevMotiveAnnotationGutter(project, annotation, revisions, editor.getGutter());
                UpToDateLineNumberProvider getUpToDateLineNumber = new UpToDateLineNumberProviderImpl(editor.getDocument(), project);
                AnnotationGutterLineConvertorProxy proxy = new AnnotationGutterLineConvertorProxy(getUpToDateLineNumber, devMotiveGutterProvider);
                editor.getGutter().registerTextAnnotation(proxy, proxy);
                getTemplatePresentation().setVisible(false);
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = getEventProject(e);
        if (project != null) {
            ServerType serverType = project.getComponent(RestService.class).getServerTypeIfAvailable();
            e.getPresentation().setEnabled(ServerType.AGM.equals(serverType));
        }
    }
}
