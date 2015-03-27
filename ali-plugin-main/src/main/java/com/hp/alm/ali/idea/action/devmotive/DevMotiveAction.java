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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.actions.AbstractVcsAction;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vfs.VirtualFile;

public class DevMotiveAction extends AbstractVcsAction {

    @Override
    protected boolean forceSyncUpdate(final AnActionEvent e) {
        return true;
    }

    @Override
    protected void actionPerformed(VcsContext context) {
        VirtualFile selectedFile = getSelectedFile(context);
        if (selectedFile == null) {
            return;
        }
        Project project = context.getProject();
        AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(selectedFile);
        if (activeVcs == null || activeVcs.getVcsHistoryProvider() == null) {
            return;
        }

        AliContentFactory.addDevMotiveContent(project, selectedFile, true);
    }

    @Override
    protected void update(VcsContext context, Presentation presentation) {
        presentation.setText("Development History (Dev Motive)");
        presentation.setEnabled(isEnabled(context));
        Project project = context.getProject();
        presentation.setVisible(project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss());
    }

    private static VirtualFile getSelectedFile(VcsContext context) {
        VirtualFile[] selectedFiles = context.getSelectedFiles();
        if (selectedFiles.length == 1) {
            return selectedFiles[0];
        } else {
            return null;
        }
    }

    private boolean isEnabled(VcsContext context) {
        VirtualFile selectedFile = getSelectedFile(context);
        if (selectedFile == null) {
            return false;
        }
        Project project = context.getProject();
        if (project == null) {
            return false;
        }
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(selectedFile);
        if (vcs == null) {
            return false;
        }
        VcsHistoryProvider vcsHistoryProvider = vcs.getVcsHistoryProvider();
        if (vcsHistoryProvider == null) {
            return false;
        }
        if (selectedFile.isDirectory() && !vcsHistoryProvider.supportsHistoryForDirectories()) {
            return false;
        }
        return canFileHaveHistory(project, selectedFile) && vcsHistoryProvider.canShowHistoryFor(selectedFile);
    }

    private static boolean canFileHaveHistory(Project project, VirtualFile file) {
        final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
        return fileStatus != FileStatus.ADDED && fileStatus != FileStatus.UNKNOWN && fileStatus != FileStatus.IGNORED;
    }
}
