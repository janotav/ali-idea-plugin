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

package com.hp.alm.ali.idea.ui.dialog;

import com.hp.alm.ali.idea.ui.NonAdjustingCaret;
import com.hp.alm.ali.idea.rest.RestException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestErrorDetailDialog extends MyDialog {

    public RestErrorDetailDialog(Project project, Exception exception) {
        super(project, "Error Detail", true, true, Arrays.asList(Button.Close));

        final JPanel areaPanel = new JPanel(new BorderLayout());
        areaPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(20, 20, 20, 20), BorderFactory.createEtchedBorder()));
        JTextPane area = new JTextPane();
        area.setCaret(new NonAdjustingCaret());
        boolean showArea = true;

        if(exception.getMessage().startsWith("<?xml ") || exception.getMessage().startsWith("<QCRestException>")) {
            Matcher matcher = Pattern.compile("^.*?<Title>(.*?)</Title>.*", Pattern.MULTILINE | Pattern.DOTALL).matcher(exception.getMessage());
            if(matcher.matches()) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                labelPanel.setBorder(new EmptyBorder(10, 15, 5, 15));
                JLabel label = new JLabel(matcher.group(1));
                // message can be very long, make sure we fit into the dialog
                Dimension size = label.getPreferredSize();
                size.width = Math.min(700, size.width);
                label.setPreferredSize(size);
                labelPanel.add(label);
                labelPanel.add(new LinkLabel("(detail)", null, new LinkListener() {
                    public void linkSelected(LinkLabel aSource, Object aLinkData) {
                        if(areaPanel.getParent() == null) {
                            getContentPane().add(areaPanel, BorderLayout.CENTER);
                        } else {
                            getContentPane().remove(areaPanel);
                        }
                        packAndCenter(800, 600, false);
                    }
                }));
                getContentPane().add(labelPanel, BorderLayout.NORTH);
                showArea = false;
                // adjust the area panel border to reflect our own
                areaPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 20, 20, 20), BorderFactory.createEtchedBorder()));
            }
        } else {
            // assume ALM 12 HTML format
            area.setEditorKit(new HTMLEditorKit());
            if(exception instanceof RestException) {
                try {
                    ((HTMLDocument)area.getDocument()).setBase(new URL(((RestException) exception).getLocation()));
                } catch (MalformedURLException mfe) {
                    // formatting will be broken
                }
            }
        }

        area.setEditable(false);
        area.setText(exception.getMessage());
        JBScrollPane scrollPane = new JBScrollPane(area);
        areaPanel.add(scrollPane, BorderLayout.CENTER);
        if(showArea) {
            getContentPane().add(areaPanel, BorderLayout.CENTER);
        }

        getRootPane().setDefaultButton(getButton(Button.Close));
        // although close is default button, hitting enter doesn't close the dialog when JTextPane holds the focus - override this
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                buttonPerformed(Button.Close);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_EDITOR_ENTER)), area);

        pack();
        Dimension size = getSize();
        size.width = Math.min(800, size.width);
        size.height = Math.min(600, size.height);
        setSize(size);
        centerOnOwner();
    }
}
