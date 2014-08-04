// (C) Copyright 2003-2014 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.checkin;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;
import java.util.HashSet;

public class GitCheckin implements RepositoryCheckin {

    @Override
    public boolean isMergingCheckin(CheckinProjectPanel checkinProjectPanel) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(checkinProjectPanel.getProject());
        for (VirtualFile root : getSelectedRoots(checkinProjectPanel)) {
            GitRepository repository = repositoryManager.getRepositoryForRoot(root);
            if (repository == null) {
                continue;
            }
            if (repository.getState() != Repository.State.NORMAL) {
                return true;
            }
        }
        return false;
    }

    private Collection<VirtualFile> getSelectedRoots(CheckinProjectPanel checkinProjectPanel) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(checkinProjectPanel.getProject());
        Collection<VirtualFile> result = new HashSet<VirtualFile>();
        for (FilePath path : ChangesUtil.getPaths(checkinProjectPanel.getSelectedChanges())) {
            VirtualFile root = vcsManager.getVcsRootFor(path);
            if (root != null) {
                result.add(root);
            }
        }
        return result;
    }
}
