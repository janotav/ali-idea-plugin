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

import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.DimensionService;
import com.intellij.ui.ScreenUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

public class MyDialog extends JDialog implements ActionListener {

    private Project project;

    public enum Button { OK, Cancel, Close, Clear, Save }

    private JPanel buttonPanel;
    private String dimensionKey;

    public MyDialog(Project project, String title, boolean modal) {
        this(project, title, modal, false);
    }

    public MyDialog(Project project, String title, boolean modal, boolean enableEscape) {
        this(project, title, modal, enableEscape, Collections.<Button>emptyList());
    }

    public MyDialog(Project project, String title, boolean modal, boolean enableEscape, List<Button> buttonList) {
        super(JOptionPane.getRootFrame(), title, modal);

        this.project = project;

        if(enableEscape) {
            ActionListener escListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    escape();
                }
            };
            getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        if(!buttonList.isEmpty()) {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            for(Button button: buttonList) {
                buttonPanel.add(create(button));
            }
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        centerOnOwner();
    }

    protected String getDimensionKey() {
        return null;
    }

    protected void restoreSizeAndLocation() {
        // Dialog's size and location: migrate to DialogWrapper to avoid need for our own code

        Point location = null;
        Dimension size = null;
        dimensionKey = getDimensionKey();
        if (dimensionKey != null) {
            location = DimensionService.getInstance().getLocation(dimensionKey, project);
            size = DimensionService.getInstance().getSize(dimensionKey, project);
        }
        if (location != null) {
            setLocation(location);
        } else {
            centerOnOwner();
        }
        if (size != null) {
            setSize(size.width, size.height);
        }

        Rectangle bounds = getBounds();
        ScreenUtil.fitToScreen(bounds);
        setBounds(bounds);
    }

    protected void buttonPerformed(Button button) {
        switch (button) {
            case Cancel:
            case Close:
                close(false);
        }
    }

    protected void addButton(Button button) {
        if(buttonPanel == null) {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        }
        buttonPanel.add(create(button));
    }

    protected JButton getButton(Button button) {
        if(buttonPanel != null) {
            for(Component comp: buttonPanel.getComponents()) {
                if(((JButton)comp).getText().equals(button.name())) {
                    return (JButton)comp;
                }
            }
        }
        return null;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        JButton source = (JButton) actionEvent.getSource();
        for(Button b: Button.values()) {
            if(b.name().equals(source.getText())) {
                buttonPerformed(b);
                return;
            }
        }
    }

    private JButton create(Button button) {
        JButton jButton = new JButton(button.name());
        jButton.addActionListener(this);
        return jButton;
    }

    protected void centerOnOwner() {
        setLocationRelativeTo(getOwner());
    }

    protected void close(boolean cleanClose) {
        if(!cleanClose) {
            JButton save = getButton(Button.Save);
            if(save != null && save.isEnabled()) {
                if(project.getComponent(EntityEditManager.class).askUser() != Messages.YES) {
                    return;
                }
            }
        }
        setVisible(false);
        dispose();
    }

    public void dispose() {
        if(dimensionKey != null) {
            DimensionService.getInstance().setSize(dimensionKey, getSize());
            DimensionService.getInstance().setLocation(dimensionKey, getLocation());
        }
        super.dispose();
    }

    protected void escape() {
        close(false);
    }

    public void setEditorTitle(final Project project, final String template, final String entityType) {
        EntityLabelService entityLabelService = project.getComponent(EntityLabelService.class);
        entityLabelService.loadEntityLabelAsync(entityType, new AbstractCachingService.DispatchCallback<String>() {
            @Override
            public void loaded(String entityLabel) {
                setTitle(MessageFormat.format(template, entityLabel));
            }
        });
    }

    protected void packAndCenter(int maxWidth, int maxHeight, boolean withScrollbar) {
        Rectangle position = getBounds();
        pack();
        if(position != null) {
            Dimension size = getSize();
            boolean widthAdjusted = false;
            if(size.width > maxWidth) {
                size.width = maxWidth;
                widthAdjusted = true;
            }
            boolean heightAdjusted = false;
            if(size.height > maxHeight) {
                size.height = maxHeight;
                heightAdjusted = true;

            }
            if(withScrollbar) {
                // try to make room for scrollbar that might have appeared
                if(heightAdjusted) {
                    size.width = Math.min(maxWidth, size.width + 15);
                }
                if(widthAdjusted) {
                    size.height = Math.min(maxHeight, size.height + 15);
                }
            }
            setBounds((position.x + position.width / 2) - size.width / 2, (position.y + position.height / 2) - size.height / 2, size.width, size.height);
        }
    }

    public void requestFocusAndAttention() {
        setVisible(false);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // no problem
                }
                setVisible(true);
                requestFocus();
            }
        });
    }
}
