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

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @see com.hp.alm.ali.idea.ui.dialog.RestErrorDetailDialog
 */
public class ErrorDialog extends JDialog implements ActionListener {

    public ErrorDialog(String message, String detail) {
        super(new JFrame(), "Error", true);

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(ok);

        JTextPane detailPane = new JTextPane();
        detailPane.setText(detail);
        final JBScrollPane pane = new JBScrollPane(detailPane);
        pane.setPreferredSize(new Dimension(600, 300));
        getContentPane().add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel();
        content.add(new JLabel(IconLoader.getIcon("/general/errorDialog.png")));
        content.add(new JLabel(message));
        content.add(new LinkLabel("(show details)", null, new LinkListener() {
            public void linkSelected(LinkLabel aSource, Object aLinkData) {
                if(pane.getParent() == getContentPane()) {
                    getContentPane().remove(pane);
                } else {
                    getContentPane().add(pane, BorderLayout.CENTER);
                }
                pack();
            }
        }));
        getContentPane().add(content, BorderLayout.NORTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
        dispose();
    }
}
