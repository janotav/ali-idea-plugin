/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.ui.editor.field;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

public class SquiggleLine extends DefaultHighlighter.DefaultHighlightPainter {

    private static final BasicStroke strokes[] = new BasicStroke[] {
            new BasicStroke(0.01F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1,3}, 2),
            new BasicStroke(0.01F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1,1}, 1),
            new BasicStroke(0.01F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1,3}, 0)
    };

    public SquiggleLine(Color color){
        super(color);
    }

    @Override
    public Shape paintLayer(Graphics g, int i, int j, Shape shape, JTextComponent comp, View view) {
        if (comp.isEditable()) {
            g.setColor(getColor());
            try {
                Shape sh = view.modelToView(i, Position.Bias.Forward, j, Position.Bias.Backward, shape);
                Rectangle rect = (sh instanceof Rectangle) ? (Rectangle)sh : sh.getBounds();
                int x = rect.x + rect.width - 1;
                int y = rect.y + rect.height - 3;
                Graphics2D g2 = (Graphics2D)g;
                for (BasicStroke stroke: strokes) {
                    g2.setStroke(stroke);
                    g2.drawLine(rect.x, y, x, y++);
                }
                return rect;
            } catch (BadLocationException e) {
                return null;
            }
        }
        return null;
    }
}