// (C) Copyright 2003-2013 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.action.changeset;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.awt.datatransfer.StringSelection;
import java.util.Collections;
import java.util.Set;

public class CopyRevisionToClipboardAction extends EntityAction {

    public CopyRevisionToClipboardAction() {
        super("Copy Revision", "Copy Changeset Revision to Clipboard", IconLoader.getIcon("/actions/copy.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("changeset");
    }

    @Override
    protected boolean enabledPredicate(Project project, Entity entity) {
        return !entity.getPropertyValue("rev").isEmpty();
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        String rev = entity.getPropertyValue("rev");
        // temporary workaround for Git revisions: strip the trailing branch name
        rev = rev.replaceFirst("^([a-fA-F0-9]{40}) \\S+$", "$1");
        CopyPasteManager.getInstance().setContents(new StringSelection(rev));
    }
}
