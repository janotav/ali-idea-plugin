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

package com.hp.alm.ali.idea.content;

import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.entity.edit.LockingStrategy;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.services.ProjectUserService;
import com.hp.alm.ali.idea.ui.ChooseEntityTypePopup;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.model.Metadata;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.actions.ContentChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AliCheckinHandler extends CheckinHandler implements ActionListener, DocumentListener, ActiveItemService.Listener {
    private JPanel header;
    private JCheckBox markFixed;
    private JComboBox markFixedSelection;
    private ActionToolbar toolbar;
    private JCheckBox addComment;
    private JPanel panelComment;
    private JScrollPane commentPane;
    private JTextArea comment;
    private JPanel panel;
    private CheckinProjectPanel checkinProjectPanel;

    private Project project;
    private RestService restService;
    private EntityRef ref;
    private AliProjectConfiguration projConf;
    private String lastAddedComment = null;

    public AliCheckinHandler(CheckinProjectPanel checkinProjectPanel) {
        this.checkinProjectPanel = checkinProjectPanel;
        this.project = checkinProjectPanel.getProject();
        this.restService = project.getComponent(RestService.class);
        final ActiveItemService activeItemService = project.getComponent(ActiveItemService.class);
        this.ref = activeItemService.getActiveItem();

        this.projConf = project.getComponent(AliProjectConfiguration.class);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("HP ALI"));

        if(!restService.getServerTypeIfAvailable().isConnected()) {
            panel.setVisible(false);
        } else if(ref == null) {
            activeItemService.addListener(this);
            DefaultActionGroup group = new DefaultActionGroup();
            ChooseEntityTypeAction choose = new ChooseEntityTypeAction(project, panel, Arrays.asList("defect", "requirement"), new ChooseEntityTypePopup.Listener() {
                @Override
                public void selected(final String entityType) {
                    FilterChooser popup = restService.getServerStrategy().getFilterChooser(entityType, false, true, false, null);
                    popup.show();
                    String selectedId = popup.getSelectedValue();
                    if (selectedId != null && !selectedId.isEmpty()) {
                        activeItemService.activate(new Entity(entityType, Integer.valueOf(selectedId)), true, false);
                    }
                }
            });
            group.add(choose);
            panel.add(new JLabel("Not associated with any work item"), BorderLayout.WEST);
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
            panel.add(toolbar.getComponent(), BorderLayout.EAST);
            choose.setComponent(toolbar.getComponent());
        } else {
            setupPanel(checkinProjectPanel, ref);
        }
    }

    private void setupPanel(CheckinProjectPanel checkinProjectPanel, final EntityRef ref) {
        // replace commit message only if necessary
        String prefix = ref.toString() + ": ";
        String msg = checkinProjectPanel.getCommitMessage();
        if(!msg.startsWith(prefix)) {
            checkinProjectPanel.setCommitMessage(prefix);
        }

        markFixed = new JCheckBox("Mark " + ref.toString() + " as ");
        if(ref.type.equals("defect")) {
            markFixedSelection = new JComboBox();

            markFixed.setEnabled(false);
            markFixedSelection.setEnabled(false);

            JPanel jPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            jPanel.add(markFixed);
            jPanel.add(markFixedSelection);
            panel.add(jPanel, BorderLayout.NORTH);

            final EntityService entityService = project.getComponent(EntityService.class);
            EntityListener callback = new EntityAdapter() {
                @Override
                public void entityLoaded(Entity entity, Event event) {
                    final List<String> allowedTransitions = projConf.getStatusTransitions().getAllowedTransitions(entity.getPropertyValue("status"));

                    Metadata metadata = project.getComponent(MetadataService.class).getEntityMetadata(ref.type);
                    final Field field = metadata.getAllFields().get("status");
                    final List<String> list = project.getComponent(ProjectListService.class).getProjectList(ref.type, field);

                    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                        public void run() {
                            for(String state: list) {
                                if(allowedTransitions.contains(state)) {
                                    markFixedSelection.addItem(state);
                                }
                            }
                            if(markFixedSelection.getItemCount() > 0) {
                                markFixedSelection.setSelectedIndex(0);
                                markFixed.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent actionEvent) {
                                        markFixedSelection.setEnabled(markFixed.isSelected());
                                    }
                                });
                                markFixed.setEnabled(true);
                                markFixedSelection.setEnabled(true);
                           }
                        }
                    });
                }
            };
            entityService.requestCachedEntity(ref, Arrays.asList("status"), callback);
        }

        comment = new JTextArea();
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.setBorder(BorderFactory.createEtchedBorder());
        comment.getDocument().addDocumentListener(this);

        final List<String> comments = projConf.getComments();
        if(!comments.isEmpty()) {
            Collections.reverse(comments);
            comment.setText(comments.get(0));
        }

        header = new JPanel(new BorderLayout());
        addComment = new JCheckBox("Add comment to "+ref.toString());
        addComment.addActionListener(this);
        header.add(addComment, BorderLayout.WEST);

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Choose Message", "Choose Recent Message", IconLoader.getIcon("/actions/consoleHistory.png")) {
            public void update(AnActionEvent e) {
                super.update(e);

                e.getPresentation().setEnabled(!comments.isEmpty());
            }

            public void actionPerformed(AnActionEvent e) {
                ContentChooser<String> chooser = new ContentChooser<String>(project, "Choose Message", false) {
                    protected void removeContentAt(final String content) {
                        projConf.getComments().remove(content);
                    }

                    protected String getStringRepresentationFor(String content) {
                        return content;
                    }

                    protected List<String> getContents() {
                        return comments;
                    }
                };

                chooser.show();

                if (chooser.isOK()) {
                    if(!addComment.isSelected()) {
                        addComment.setSelected(true);
                        showCommentPane();
                    }

                    int selectedIndex = chooser.getSelectedIndex();

                    if (selectedIndex >= 0) {
                        comment.setText(chooser.getAllContents().get(selectedIndex));
                    }
                }
            }
        });

        toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        header.add(toolbar.getComponent(), BorderLayout.EAST);

        panelComment = new JPanel(new BorderLayout());
        panelComment.add(header, BorderLayout.NORTH);
        commentPane = new JBScrollPane(comment);

        panel.add(panelComment, BorderLayout.CENTER);

        // requirement doesn't show up in Idea12 unless explicitly revalidated
        panel.revalidate();
    }

    public void checkinSuccessful() {
        if(markFixed == null) {
            // no associated work item
            return;
        }
        if(markFixed.isSelected() || (addComment.isSelected() && !comment.getText().isEmpty())) {
            EntityService entityService = project.getComponent(EntityService.class);
            LockingStrategy lockingStrategy = restService.getServerStrategy().getLockingStrategy();
            Entity entity = lockingStrategy.lock(ref.toEntity());
            if(entity != null) {
                Set<String> modified = new HashSet<String>();
                if(markFixed.isSelected()) {
                    String value = markFixedSelection.getSelectedItem().toString();
                    if(!value.equals(entity.getProperty("status"))) {
                        entity.setProperty("status", value);
                        modified.add("status");
                    }
                }
                if(addComment.isSelected() && !comment.getText().isEmpty()) {
                    String userName = project.getComponent(AliProjectConfiguration.class).getUsername();
                    String fullName = project.getComponent(ProjectUserService.class).getUser(userName).getFullName();

                    String commentProperty = entity.isInitialized("dev-comments")? "dev-comments": "comments";
                    String mergedComment = CommentField.mergeComment(entity.getPropertyValue(commentProperty), comment.getText(), userName, fullName);
                    entity.setProperty(commentProperty, mergedComment);
                    modified.add(commentProperty);
                }
                entityService.updateEntity(entity, modified, false);
                lockingStrategy.unlock(entity);
            }
        }
    }

    public RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable disposable) {
        return new RefreshableOnComponent() {
            public JComponent getComponent() {
                return panel;
            }

            public void refresh() {
            }

            public void saveState() {
            }

            public void restoreState() {
            }
        };
    }

    private void showCommentPane() {
        panelComment.add(commentPane, BorderLayout.CENTER);
        panelComment.setSize(new Dimension(panel.getWidth() - 20, 120));
        panelComment.setPreferredSize(new Dimension(panel.getWidth() - 20, 120));
        panelComment.setMaximumSize(new Dimension(panel.getWidth() - 20, 120));
        panelComment.revalidate();
        panelComment.repaint();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if(addComment.isSelected()) {
            showCommentPane();
        } else {
            panelComment.remove(commentPane);
            panelComment.setPreferredSize(null);
            panelComment.revalidate();
            panelComment.repaint();
        }
    }

    public void insertUpdate(DocumentEvent documentEvent) {
        storeComment();
    }

    public void removeUpdate(DocumentEvent documentEvent) {
        storeComment();
    }

    public void changedUpdate(DocumentEvent documentEvent) {
        storeComment();
    }

    private void storeComment() {
        if(lastAddedComment != null) {
            projConf.removeComment(lastAddedComment);
        }
        if(projConf.addComment(comment.getText())) {
            lastAddedComment = comment.getText();
        }
    }

    public void onActivated(EntityRef ref) {
        if(ref != null) {
            this.ref = ref;
            panel.removeAll();
            setupPanel(checkinProjectPanel, ref);
        }
    }

    public static class ChooseEntityTypeAction extends AnAction {

        private static Icon icon = IconLoader.getIcon("/actions/quickList.png");

        private Project project;
        private JComponent component;
        private List<String> entityTypes;
        private ChooseEntityTypePopup.Listener listener;

        public ChooseEntityTypeAction(Project project, JComponent component, List<String> entityTypes, ChooseEntityTypePopup.Listener listener) {
            this.project = project;
            this.component = component;
            this.entityTypes = entityTypes;
            this.listener = listener;

            getTemplatePresentation().setIcon(icon);
        }

        public void setComponent(JComponent component) {
            this.component = component;
        }

        @Override
        public void actionPerformed(AnActionEvent anActionEvent) {
            ChooseEntityTypePopup popup = new ChooseEntityTypePopup(project, entityTypes, listener);
            popup.showOrInvokeDirectly(component, 0, 0);
        }
    }
}
