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

package com.hp.alm.ali.idea.ui.entity;

import com.hp.alm.ali.idea.entity.EntityStatusIndicator;
import com.hp.alm.ali.idea.ui.dialog.ErrorDialog;
import com.hp.alm.ali.idea.ui.dialog.RestErrorDetailDialog;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.RestException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.PrintWriter;
import java.io.StringWriter;

public class EntityStatusPanel extends JPanel implements EntityStatusIndicator, LinkListener {

    private static Icon redoIcon = IconLoader.getIcon("/actions/sync.png");
    private static Icon detailsIcon = IconLoader.getIcon("/general/error.png");

    private JLabel icon;
    private JLabel message;
    private LinkLabel detail;
    private Exception exception;

    private Runnable redo;

    public EntityStatusPanel(final Project project) {
        super(new FlowLayout(FlowLayout.LEFT, 3, 0));
        setOpaque(false);

        icon = new LinkLabel();
        message = new JLabel("");
        detail = new LinkLabel("", detailsIcon, new LinkListener() {
            public void linkSelected(LinkLabel aSource, Object aLinkData) {
                if(exception instanceof RestException) {
                    new RestErrorDetailDialog(project, exception).setVisible(true);
                } else {
                    StringWriter sw = new StringWriter();
                    exception.printStackTrace(new PrintWriter(sw));
                    new ErrorDialog(exception.getMessage() == null? exception.getClass().getSimpleName(): exception.getMessage(), sw.toString()).setVisible(true);
                }
            }
        }, null);

        add(message);
    }

    @Override
    public void loading() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                setIcon(null);
                handleException(null);
                message.setText("Loading information...");
            }
        });
    }

    @Override
    public void loaded(final EntityList data, Runnable redo) {
        info("Loaded " + getItemCountString(data, "items"), null, redo);
    }

    @Override
    public void clear() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                message.setText("");
                handleRedo(null);
                handleException(null);
            }
        });
    }

    @Override
    public void info(final String msg, final Exception e, final Runnable redo) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                message.setText(msg);
                handleRedo(redo);
                handleException(e);
            }
        });
    }

    public static String getItemCountString(EntityList data, String name) {
        if (data.getTotal() > data.size()) {
            return data.size() + " " + name + " out of " + data.getTotal();
        } else {
            return data.size() + " " + name;
        }
    }

    private void setIcon(Icon i) {
        if(icon.getParent() != null) {
            if(i != null && i.equals(icon.getIcon())) {
                return;
            }
            remove(icon);
        }
        if(i != null) {
            icon = new LinkLabel("", i, this, null);
            add(icon, 0);
        }
    }

    private void handleException(Exception e) {
        this.exception = e;
        if(e != null) {
            if(detail.getParent() == null) {
                add(detail);
            }
        } else {
            if(detail.getParent() != null) {
                remove(detail);
            }
        }
    }

    private void handleRedo(Runnable redo) {
        this.redo = redo;
        if(redo != null) {
            setIcon(redoIcon);
        } else {
            setIcon(null);
        }
    }

    @Override
    public void linkSelected(LinkLabel aSource, Object aLinkData) {
        redo.run();
    }

    @Override
    public Dimension getPreferredSize() {
        // avoid visual flicker caused by resizing when nothing/something is showing
        Dimension size = super.getPreferredSize();
        Insets insets;
        Border border = getBorder();
        if(border != null) {
            insets = border.getBorderInsets(this);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }
        return new Dimension(size.width, Math.max(size.height, detailsIcon.getIconHeight() + insets.bottom + insets.top));
    }
}
