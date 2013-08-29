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

import com.intellij.openapi.util.IconLoader;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class GridBagSplitter extends MouseAdapter {
    private Point startPosition;
    private int[] rowHeights;
    private double[] rowWeights;
    private int firstComponent;
    private final GridBagLayout layout;
    private final int pos;
    private JPanel gridPanel;
    private Window window;
    private JLabel splitter;

    public GridBagSplitter(Window window, JPanel gridPanel, int pos) {
        this.window = window;
        this.gridPanel = gridPanel;
        this.pos = pos;
        this.layout = (GridBagLayout) gridPanel.getLayout();

        splitter = new JLabel(IconLoader.getIcon("/general/splitGlueV.png"));
        splitter.addMouseListener(this);
        splitter.addMouseMotionListener(this);
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        if(startPosition == null) {
            rowHeights = layout.getLayoutDimensions()[1];
            rowWeights = layout.getLayoutWeights()[1];

            firstComponent = - 1;
            for(int i = pos; i > 0; i--) {
                if(rowWeights[i] > 0) {
                    firstComponent = i;
                    break;
                }
            }

            if(firstComponent >= 0) {
                window.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            }
        }
    }

    public void mouseExited(MouseEvent mouseEvent) {
        if(startPosition == null) {
            window.setCursor(Cursor.getDefaultCursor());
        }

    }

    public void mousePressed(MouseEvent mouseEvent) {
        if(firstComponent >= 0) {
            startPosition = mouseEvent.getLocationOnScreen();
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        if(startPosition != null) {
            window.setCursor(Cursor.getDefaultCursor());
            startPosition = null;
        }
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        if(startPosition != null) {
            Point endPosition = mouseEvent.getLocationOnScreen();

            Component first = getComponentForRow(firstComponent);
            Component second = getComponentForRow(pos + 1);

            int diff = Math.max(endPosition.y - startPosition.y, first.getPreferredSize().height - rowHeights[firstComponent]);
            diff = Math.min(diff, rowHeights[pos + 1] - second.getPreferredSize().height);

            for(int i = 0; i < rowHeights.length; i++) {
                if(rowWeights[i] > 0) {
                    Component comp = getComponentForRow(i);
                    double weight = (double) (rowHeights[i] - comp.getPreferredSize().height);
                    if(i == firstComponent) {
                        weight += diff;
                    } else if(i == pos + 1) {
                        weight -= diff;
                    }
                    // make sure weight remains > 0 for the layout to continue working properly
                    setComponentWeight(layout, Math.max(0.00001, weight), comp);
                }
            }

            layout.layoutContainer(gridPanel);
            gridPanel.revalidate();
            gridPanel.repaint();
        }
    }

    protected abstract Component getComponentForRow(int n);

    private void setComponentWeight(GridBagLayout layout, double weighty, Component component) {
        GridBagConstraints c = layout.getConstraints(component);
        c.weighty = weighty;
        layout.setConstraints(component, c);
    }

    public Component getComponent() {
        return splitter;
    }
}
