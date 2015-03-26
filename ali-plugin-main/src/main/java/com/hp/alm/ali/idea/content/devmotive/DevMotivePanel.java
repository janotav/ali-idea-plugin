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

import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.content.CloseableContent;
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.DevMotiveService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.DateSelectorLabel;
import com.hp.alm.ali.idea.ui.MultiValueSelectorLabel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.committed.RepositoryChangesBrowser;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsFileRevisionEx;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DevMotivePanel extends JPanel implements CloseableContent, LinkListener, ChangeListener, HyperlinkListener {

    private static final int REVISION_BATCH_SIZE = 10;

    private final Project project;
    private final VirtualFile file;

    private final DevMotiveService devMotiveService;
    private final EntityService entityService;
    private final IncompleteWarningPanel incompleteWarningPanel;
    private JPanel filterPanel;

    private MultiValueSelectorLabel userSelector;
    private DateSelectorLabel dateSelector;
    private Object filterId = new Object();

    private JTable workItemsTable;
    private WorkItemsTableModel workItemsTableModel;

    private JTable commitsTable;
    private CommitsTableModel commitsTableModel;

    private RepositoryChangesBrowser fileChanges;
    private Map<VcsRevisionNumber, List<Change>> vcsCache;

    private MultiMap<WorkItem, Commit> workItemToCommits;

    private List<Commit> allCommits;
    private List<Commit> filteredCommits;
    private Set<Commit> processedCommits;

    public DevMotivePanel(final Project project, final VirtualFile file) {
        super(new BorderLayout());

        this.project = project;
        this.file = file;

        devMotiveService = project.getComponent(DevMotiveService.class);
        entityService = project.getComponent(EntityService.class);

        workItemToCommits = new MultiMap<WorkItem, Commit>();
        vcsCache = new HashMap<VcsRevisionNumber, List<Change>>();

        commitsTableModel = new CommitsTableModel();
        commitsTable = new JBTable(commitsTableModel);
        commitsTable.setDefaultRenderer(Date.class, new DateRenderer());
        commitsTable.setAutoCreateRowSorter(true);
        commitsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setColumn(commitsTable, 0, 150, true);
        setColumn(commitsTable, 1, 250, true);
        setColumn(commitsTable, 2, 1000, false);
        commitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    List<Change> allChanges = new LinkedList<Change>();
                    ListSelectionModel selectionModel = commitsTable.getSelectionModel();
                    if (!selectionModel.isSelectionEmpty()) {
                        for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                            boolean selected = selectionModel.isSelectedIndex(i);
                            if (selected) {
                                Commit commit = commitsTableModel.getCommit(i);
                                List<Change> changes = getChangeList(commit.getRevisionNumber());
                                allChanges.addAll(changes);
                            }
                        }
                    }
                    fileChanges.setChangesToDisplay(allChanges);
                }
            }
        });

        workItemsTableModel = new WorkItemsTableModel(entityService);
        workItemsTable = new JBTable(workItemsTableModel);
        // TODO: show detail action into toolbar
        workItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int selectedRow = workItemsTable.getSelectedRow();
                    if(selectedRow >= 0) {
                        int idx = workItemsTable.convertRowIndexToModel(selectedRow);
                        WorkItem workItem = workItemsTableModel.getWorkItem(idx);
                        if (workItem.getType() != WorkItem.Type.NONE) {
                            EntityRef entityRef = workItem.toEntityRef();
                            AliContentFactory.loadDetail(project, entityRef.toEntity(), true, true);
                        }
                    }
                }
            }
        });
        workItemsTable.setDefaultRenderer(WorkItem.class, new WorkItemRenderer());
        workItemsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setColumn(workItemsTable, 0, 75, true);
        setColumn(workItemsTable, 1, 75, true);
        setColumn(workItemsTable, 2, 1000, false);
        workItemsTable.setAutoCreateRowSorter(true);
        workItemsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    for (int i = 0; i < workItemsTableModel.getRowCount(); i++) {
                        WorkItem workItem = workItemsTableModel.getWorkItem(i);
                        List<Commit> filteredCommits = new LinkedList<Commit>();
                        filterCommits(workItemToCommits.get(workItem), filteredCommits, null);
                        boolean selected = workItemsTable.getSelectionModel().isSelectedIndex(i);
                        if (selected) {
                            commitsTableModel.add(filteredCommits);
                        } else {
                            commitsTableModel.remove(filteredCommits);
                        }
                    }
                }
            }
        });

        fileChanges = new RepositoryChangesBrowser(project, Collections.<CommittedChangeList>emptyList());

        incompleteWarningPanel = new IncompleteWarningPanel(getBackground());
        incompleteWarningPanel.addHyperLinkListener(this);
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JPanel statusAndFilter = new JPanel(new BorderLayout());
        statusAndFilter.add(incompleteWarningPanel, BorderLayout.NORTH);
        statusAndFilter.add(filterPanel, BorderLayout.SOUTH);
        JPanel upPanel = new JPanel(new BorderLayout());
        upPanel.add(statusAndFilter, BorderLayout.NORTH);
        upPanel.add(new JBScrollPane(workItemsTable), BorderLayout.CENTER);
        JBSplitter upAndDown = new JBSplitter(true);
        upAndDown.setFirstComponent(upPanel);
        upAndDown.setSecondComponent(new JBScrollPane(commitsTable));

        JBSplitter leftAndRight = new JBSplitter(false);
        leftAndRight.setFirstComponent(upAndDown);
        leftAndRight.setSecondComponent(fileChanges);
        add(leftAndRight, BorderLayout.CENTER);

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                initialize();
            }
        });
    }

    private List<Change> getChangeList(VcsRevisionNumber revisionNumber) {
        List<Change> changes = vcsCache.get(revisionNumber);
        if (changes == null) {
            try {
                changes = new LinkedList<Change>();
                AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
                Pair oneList = vcs.getCommittedChangesProvider().getOneList(file, revisionNumber);
                changes.addAll(((ChangeList) oneList.getFirst()).getChanges());
            } catch (VcsException ex) {
            }
            vcsCache.put(revisionNumber, changes);
        }
        return changes;
    }

    private void filterCommits(Collection<Commit> commits, List<Commit> filtered, List<Commit> unfiltered) {
        HashSet<String> users = new HashSet<String>(userSelector.getSelectedValues());
        Long lowerBound = dateSelector.getLowerBound();
        Long upperBound = dateSelector.getUpperBound();
        for (Commit commit: commits) {
            if (!revisionMatches(commit.getRevision(), users)) {
                if (unfiltered != null) {
                    unfiltered.add(commit);
                }
                continue;
            }

            if (lowerBound != null && commit.getDate().getTime() < lowerBound) {
                if (unfiltered != null) {
                    unfiltered.add(commit);
                }
                continue;
            }

            if (upperBound != null && commit.getDate().getTime() > upperBound) {
                if (unfiltered != null) {
                    unfiltered.add(commit);
                }
                continue;
            }

            filtered.add(commit);
        }
    }

    private synchronized void applyFilter() {
        filterId = new Object();
        filteredCommits.clear();
        filterCommits(allCommits, filteredCommits, null);

        for (WorkItem workItem: workItemToCommits.keySet()) {
            Collection<Commit> commits = workItemToCommits.getModifiable(workItem);
            LinkedList<Commit> matched = new LinkedList<Commit>();
            LinkedList<Commit> unmatched = new LinkedList<Commit>();
            filterCommits(commits, matched, unmatched);
            if (!matched.isEmpty()) {
                // work item should be visible, some commits may no longer apply though
                addWorkItem(workItem, matched, unmatched);
            } else {
                // work item is filtered out, remove all commits from the table
                workItemsTableModel.removeWorkItem(workItem);
                commitsTableModel.remove(commits);
            }
        }

        int processed = countProcessed();
        incompleteWarningPanel.setState(processed, filteredCommits.size());

        if (processed < REVISION_BATCH_SIZE && filteredCommits.size() > processed) {
            loadNext();
        }
    }

    private void addWorkItem(WorkItem workItem, Commit commit) {
        addWorkItem(workItem, Arrays.asList(commit), Collections.<Commit>emptyList());
    }

    private void addWorkItem(WorkItem workItem, Collection<Commit> matched, Collection<Commit> unmatched) {
        int idx = workItemsTableModel.addWorkItem(workItem);
        int row = workItemsTable.convertRowIndexToView(idx);
        if (workItemsTable.getSelectionModel().isSelectedIndex(row)) {
            // and it is selected
            commitsTableModel.add(matched);
            commitsTableModel.remove(unmatched);
        }
    }

    private boolean revisionMatches(VcsFileRevision revision, Set<String> selectedUsers) {
        if (!selectedUsers.isEmpty() && !selectedUsers.contains(revision.getAuthor())) {
            return false;
        }

        return true;
    }

    private void setColumn(JTable table, int index, int width, boolean setMinimum) {
        table.getColumnModel().getColumn(index).setPreferredWidth(width);
        if (setMinimum) {
            table.getColumnModel().getColumn(index).setMinWidth(width);
        }
    }

    private void initialize() {
        filteredCommits = new ArrayList<Commit>();
        allCommits = new LinkedList<Commit>();
        processedCommits = new HashSet<Commit>();

        List<VcsFileRevision> revisions = loadRevisions();
        if (revisions != null) {
            for (VcsFileRevision revision: revisions) {
                String committer = null;
                String committerEmail = null;
                String authorEmail = null;
                if (revision instanceof VcsFileRevisionEx) {
                    committer = ((VcsFileRevisionEx) revision).getCommitterName();
                    committerEmail = ((VcsFileRevisionEx) revision).getCommitterEmail();
                    authorEmail = ((VcsFileRevisionEx) revision).getAuthorEmail();
                }
                String author = revision.getAuthor();
                allCommits.add(new Commit(
                        revision,
                        author,
                        authorEmail,
                        committer,
                        committerEmail));

            }

            filteredCommits.addAll(allCommits);
            final Set<String> users = new HashSet<String>();
            if (!filteredCommits.isEmpty()) {
                for (Commit commit: filteredCommits) {
                    users.add(commit.getAuthorName());
                }
                handleNextBatch();
            }
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    initFilterPanel(new LinkedList<String>(users));
                }
            });
        } else {
            incompleteWarningPanel.markFailed();
        }
    }

    private List<VcsFileRevision> loadRevisions() {
        try {
            AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
            if (vcs == null) {
                return null;
            }
            VcsHistoryProvider vcsHistoryProvider = vcs.getVcsHistoryProvider();
            if (vcsHistoryProvider == null) {
                return null;
            }
            VcsHistorySession historySession = vcsHistoryProvider.createSessionFor(new FilePathImpl(file));
            if (historySession == null) {
                return null;
            }
            return historySession.getRevisionList();
        } catch (VcsException e) {
            return null;
        }
    }

    private void initFilterPanel(List<String> users) {
        userSelector = new MultiValueSelectorLabel(project, "Author", users, users);
        userSelector.setMaximumWidth(20);
        userSelector.addChangeListener(this);
        filterPanel.add(userSelector);

        dateSelector = new DateSelectorLabel(project, "Date");
        dateSelector.addChangeListener(this);
        filterPanel.add(dateSelector);
    }

    private void handleNextBatch() {
        List<Commit> commits = new LinkedList<Commit>();
        final Object filterId;
        synchronized (this) {
            filterId = this.filterId;
            for (Commit commit: filteredCommits) {
                if (processedCommits.contains(commit)) {
                    continue;
                }

                commits.add(commit);
                if (commits.size() == REVISION_BATCH_SIZE) {
                    break;
                }
            }
        }
        if (commits.isEmpty()) {
            return;
        }
        final Map<Commit, List<EntityRef>> commitToWorkItems = devMotiveService.getRelatedWorkItems(commits);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                synchronized (DevMotivePanel.this) {
                    boolean myQuery = filterId.equals(DevMotivePanel.this.filterId);
                    for (Commit commit: commitToWorkItems.keySet()) {
                        processedCommits.add(commit);
                        List<EntityRef> workItemRefs = commitToWorkItems.get(commit);
                        if (workItemRefs == null) {
                            WorkItem unresolved = WorkItem.unresolved();
                            workItemToCommits.getModifiable(unresolved).add(commit);
                            if (myQuery) {
                                addWorkItem(unresolved, commit);
                            }
                        } else if (workItemRefs.isEmpty()) {
                            WorkItem unassigned = WorkItem.unassigned();
                            workItemToCommits.getModifiable(unassigned).add(commit);
                            if (myQuery) {
                                addWorkItem(unassigned, commit);

                            }
                        } else {
                            for (EntityRef workItemRef: workItemRefs) {
                                WorkItem item = new WorkItem(workItemRef.type, workItemRef.id);
                                workItemToCommits.getModifiable(item).add(commit);
                                if (myQuery) {
                                    addWorkItem(item, commit);
                                }
                            }
                        }
                    }
                    incompleteWarningPanel.setState(countProcessed(), filteredCommits.size());
                }
            }
        });
    }

    @Override
    public void linkSelected(LinkLabel aSource, Object aLinkData) {
        loadNext();
    }

    private void loadNext() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                handleNextBatch();
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        applyFilter();
    }

    private int countProcessed() {
        int processed = 0;
        for (Commit commit: filteredCommits) {
            if (processedCommits.contains(commit)) {
                ++processed;
            }
        }
        return processed;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            loadNext();
        }
    }

    private static class DateRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String dateStr = DateFormatUtil.formatPrettyDateTime((Date) value);
            return super.getTableCellRendererComponent(table, dateStr, isSelected, hasFocus, row, column);
        }
    }

    private class WorkItemRenderer extends DefaultTableCellRenderer {

        public DefaultTableCellRenderer getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                      boolean hasFocus, int row, int column) {
            final WorkItem workItem = (WorkItem) value;
            String name = workItem.getName();
            if (name == null) {
                name = "Loading...";
                workItem.setName(name);
                final int modelRow = workItemsTable.convertRowIndexToModel(row);
                entityService.requestCachedEntity(workItem.toEntityRef(), Arrays.asList("name"), new EntityAdapter() {
                    @Override
                    public void entityLoaded(final Entity entity, Event event) {
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            @Override
                            public void run() {
                                workItem.setName(entity.getPropertyValue("name"));
                                workItemsTableModel.fireTableRowsUpdated(modelRow, modelRow);
                            }
                        });
                    }

                    @Override
                    public void entityNotFound(EntityRef ref, boolean removed) {
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            @Override
                            public void run() {
                                workItem.setName("Error: Entity not found");
                                workItemsTableModel.fireTableRowsUpdated(modelRow, modelRow);
                            }
                        });
                    }
                });
            }
            DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, name, isSelected, hasFocus, row, column);
            if (workItem.getType() == WorkItem.Type.NONE) {
                renderer.setFont(renderer.getFont().deriveFont(Font.ITALIC));
            }
            return renderer;
        }
    }
}
