// (C) Copyright 2003-2013 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.revision.RevisionFactory;
import com.hp.alm.ali.idea.services.EntityService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.annotate.ShowAllAffectedGenericAction;
import com.intellij.openapi.vcs.changes.TextRevisionNumber;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class ShowAffectedPathsAction extends EntityAction {

    public ShowAffectedPathsAction() {
        super("Paths", "Show Affected Paths", IconLoader.getIcon("/diff/Diff.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("changeset");
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, final Entity entity) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Show Affected Paths", true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                EntityQuery query = new EntityQuery("changeset-file");
                query.addColumn("scm-repository", 1);
                query.setValue("parent-id", String.valueOf(entity.getId()));
                EntityService entityService = project.getComponent(EntityService.class);
                EntityList list = entityService.query(query);
                if (indicator.isCanceled() || list.isEmpty()) {
                    return;
                }
                entityService.requestCachedEntity(new EntityRef("scm-repository", Integer.valueOf(list.get(0).getPropertyValue("scm-repository"))), Arrays.asList("location", "alias"), new EntityAdapter() {
                    @Override
                    public void entityLoaded(Entity repository, Event event) {
                        if (indicator.isCanceled()) {
                            return;
                        }
                        VcsRoot vcsRoot = findRepository(project, repository);
                        if (vcsRoot != null) {
                            showInVcsRoot(project, vcsRoot, entity);
                        } else {
                            VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
                            for (VcsRoot root : vcsRoots) {
                                if (showInVcsRoot(project, root, entity)) {
                                    break;
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    private boolean showInVcsRoot(final Project project, final VcsRoot root, Entity changeset) {
        try {
            final AbstractVcs vcs = root.getVcs();
            final VcsRevisionNumber revision = createRevision(vcs.getName(), changeset.getPropertyValue("rev"));
            if(revision != null && vcs.getCommittedChangesProvider().getOneList(root.getPath(), revision).getFirst() != null) {
                UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        ShowAllAffectedGenericAction.showSubmittedFiles(project, revision, root.getPath(), vcs.getKeyInstanceMethod());
                    }
                });
                return true;
            }
        } catch (VcsException e) {
            // TODO: may not be fetched yet, warn
        }
        return  false;
    }

    private VcsRoot findRepository(Project project, Entity repository) {
        String location = repository.getPropertyValue("location");
        String alias = repository.getPropertyValue("alias");

        VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
        for(VcsRoot root: vcsRoots) {
            RevisionFactory revisionFactory = findFactory(root.getVcs().getName());
            if(revisionFactory != null && revisionFactory.matches(root, location, alias)) {
                return root;
            }
        }

        return null;
    }

    private RevisionFactory findFactory(String vcsName) {
        RevisionFactory[] revisionFactories = Extensions.getExtensions(RevisionFactory.EXTENSION_POINT_NAME);
        for(RevisionFactory factory: revisionFactories) {
            if(factory.getType().equals(vcsName)) {
                return factory;
            }
        }
        return null;
    }

    private VcsRevisionNumber createRevision(String vcsName, String revision) {
        RevisionFactory revisionFactory = findFactory(vcsName);
        if(revisionFactory != null) {
            return revisionFactory.create(revision);
        } else {
            // not recommended, but it may actually work
            return new TextRevisionNumber(revision);
        }
    }
}
