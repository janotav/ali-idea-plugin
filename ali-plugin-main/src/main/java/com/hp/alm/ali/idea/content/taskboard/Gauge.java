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

package com.hp.alm.ali.idea.content.taskboard;

import com.hp.alm.ali.idea.ui.FixedLabel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class Gauge extends JPanel {
    private JPanel done;
    private JPanel todo;
    private JLabel label;
    private double value;

    public Gauge(double val) {
        super(new GridBagLayout());

        assertValidRange(val);

        setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = val;
        c.fill = GridBagConstraints.HORIZONTAL;
        done = new FixedPanel();
        done.setBackground(new Color(0x66, 0x99, 0x00));
        add(done, c);
        c.gridx++;
        c.weightx = 1 - val;
        todo = new FixedPanel();
        todo.setBackground(new Color(0x33, 0x99, 0xFF));
        add(todo, c);
        c.gridx++;
        c.weightx = 0;
        label = new FixedLabel(getPercentage() + "%", 60);
        Font font = label.getFont()
                .deriveFont(18.0f)
                .deriveFont(Font.BOLD);
        label.setFont(font);
        label.setHorizontalAlignment(JLabel.RIGHT);
        add(label, c);
    }

    public void setValue(double val) {
        assertValidRange(val);

        this.value = val;
        GridBagLayout layout = (GridBagLayout) getLayout();
        GridBagConstraints c = layout.getConstraints(done);
        c.weightx = val;
        done.setVisible(val > 0);
        layout.setConstraints(done, c);
        c = layout.getConstraints(todo);
        c.weightx = 1 - val;
        todo.setVisible(val < 1);
        layout.setConstraints(todo, c);
        label.setText(getPercentage() + "%");
        revalidate();
        repaint();
    }

    private void assertValidRange(double val) {
        if(val < 0 || val > 1) {
            throw new IllegalArgumentException("expected 0 <= val <= 1");
        }
    }

    public double getValue() {
        return value;
    }

    public int getPercentage() {
        return (int)Math.round(value * 100);
    }

    private static class FixedPanel extends JPanel {
        public Dimension getPreferredSize() {
            return new Dimension(1, 10);
        }
    }
}
