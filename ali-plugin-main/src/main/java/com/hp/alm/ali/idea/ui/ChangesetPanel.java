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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.impl.GotoFileModel;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.ui.event.KeyboardStateFollower;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.regex.Pattern;

public class ChangesetPanel extends JPanel {

    public static final String OPERATION_ADDED = "Added";
    public static final String OPERATION_MODIFIED = "Modified";
    public static final String OPERATION_REMOVED = "Removed";

    private Project project;
    private RestService restService;

    private String revStr;
    private String descStr;
    private String ownerStr;
    private boolean descriptionFilter;

    public ChangesetPanel(Project project, Entity changeset, boolean descriptionFilter) {
        this.project = project;
        this.descriptionFilter = descriptionFilter;

        restService = project.getComponent(RestService.class);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel();
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK), BorderFactory.createEmptyBorder(5, 0, 0, 0)));
        header.setLayout(new GridBagLayout());
        header.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        revStr = changeset.getPropertyValue("rev");
        JLabel revision = new FixedLabel("Rev. "+ revStr, 50);
        revision.setHorizontalAlignment(JLabel.LEFT);
        revision.setForeground(Color.GRAY);
        c.weightx = 0.2;
        header.add(revision, c);
        c.gridx++;

        descStr = changeset.getPropertyValue("description");
        JLabel description = new FixedLabel(descStr, 250);
        c.weightx = 0.6;
        header.add(description, c);
        c.gridx++;

        ownerStr = changeset.getPropertyValue("owner");
        JLabel author = new FixedLabel(ownerStr, 180);
        author.setFont(author.getFont().deriveFont(Font.BOLD));
        c.weightx = 0.2;
        header.add(author, c);
        c.gridx++;

        JLabel date = new FixedLabel(changeset.getPropertyValue("date"), 140);
        c.weightx = 0.0;
        header.add(date);

        add(header);
    }

    private boolean matches(String filter) {
        if(filter == null) {
            return true;
        }

        if(containsIgnoreCase(ownerStr, filter) || containsIgnoreCase(revStr,  filter)) {
            return true;
        }

        if(descriptionFilter && containsIgnoreCase(descStr, filter)) {
            return true;
        }

        return false;
    }

    private boolean containsIgnoreCase(String str, String substr) {
        return Pattern.compile(Pattern.quote(substr), Pattern.CASE_INSENSITIVE).matcher(str).find();
    }

    public void addFiles(List<Entity> files, String filter) {
        boolean matches = matches(filter);
        for(final Entity file: files) {
            final String filePath = file.getPropertyValue("path");

            if(!matches && !containsIgnoreCase(filePath, filter)) {
                continue;
            }

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            panel.setBackground(Color.WHITE);

            final String operationFull = file.getPropertyValue("operation");
            String operation = operationFull.substring(0, 1);
            if("FILE".equals(file.getPropertyValue("file-type")) && (OPERATION_MODIFIED.equals(operationFull) || OPERATION_ADDED.equals(operationFull))) { // TODO: support replace
                LinkLabel action = new LinkLabel("["+ operation +"]", null);
                action.setListener(new LinkListener() {
                    @Override
                    public void linkSelected(LinkLabel aSource, Object aLinkData) {
                        if(file.getPropertyValue("diff-link").isEmpty() || KeyboardStateFollower.getState().isShiftDown()) {
                            restService.launchProjectUrl("scm/file-diff?file=" + file.getId());
                        } else {
                            BrowserUtil.launchBrowser(file.getPropertyValue("diff-link"));
                        }
                    }
                }, null);
                panel.add(action);
            } else {
                panel.add(new JLabel("["+ operation +"]"));
            }

            LinkLabel path = new LinkLabel(filePath, null);
            path.setListener(new LinkListener() {
                @Override
                public void linkSelected(LinkLabel aSource, Object aLinkData) {
                    if(!KeyboardStateFollower.getState().isCtrlDown() && !KeyboardStateFollower.getState().isShiftDown()) {
                        String fileName = filePath.replaceFirst(".*[/\\\\]", "");
                        ChooseByNameModel gotoFileModel = GotoFileModel.getGotoFileModel(project);
                        Object[] elems = gotoFileModel.getElementsByName(fileName, false, fileName);

                        // out of the many possibilities, take the one with longest match (better idea?)
                        // try to figure out actual mapping is probably too difficult
                        String filePathReverted = new StringBuffer(filePath).reverse().toString();
                        Object chosenElem = null;
                        int longestMatch = 0;
                        for(Object elem: elems) {
                            String fullNameReverted = new StringBuffer(gotoFileModel.getFullName(elem)).reverse().toString();
                            int match = prefixLength(fullNameReverted, filePathReverted);
                            if(chosenElem == null || match > longestMatch) {
                                chosenElem = elem;
                                longestMatch = match;
                            }
                        }
                        if(chosenElem instanceof NavigationItem) {
                            EditSourceUtil.navigate((NavigationItem)chosenElem, true, true);
                            return;
                        }
                    }

                    if(!file.getPropertyValue("file-link").isEmpty() && !KeyboardStateFollower.getState().isShiftDown()) {
                        BrowserUtil.launchBrowser(file.getPropertyValue("file-link"));
                    } else if(OPERATION_REMOVED.equals(operationFull)) {
                        Messages.showInfoMessage("Couldn't locate file in the project.", "Not Available");
                    } else {
                        restService.launchProjectUrl("scm/file-view?file=" + file.getId());
                    }
                }
            }, null);
            panel.add(path);

            JLabel branch = new JLabel(file.getPropertyValue("branch"));
            panel.add(branch);

            add(panel);
        }

        revalidate();
        repaint();
    }

    private int prefixLength(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        int i;
        for(i = 0; i < len1 && i < len2; i++) {
            if(str1.charAt(i) != str2.charAt(i)) {
                break;
            }
        }
        return i;
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
