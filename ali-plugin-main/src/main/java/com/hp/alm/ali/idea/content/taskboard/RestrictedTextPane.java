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

import com.hp.alm.ali.idea.ui.NonAdjustingCaret;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Rectangle;

/**
 * Poor man's attempt on fixed-width word wrapping multi-line text area with ellipsis support.
 */
public class RestrictedTextPane extends JTextPane {

    private String fullText;

    public RestrictedTextPane(int width, int height) {
        setSize(new Dimension(width, height));
        setCaret(new NonAdjustingCaret());
        setEditable(false);
        setText("");
        setOpaque(false);
    }

    public void setText(String text) {
        this.fullText = text;
        setToolTipText(text);
        trimText();
    }

    public Dimension getPreferredSize() {
        return getSize();
    }

    public void trimText() {
        super.setText(fullText);
        String text = getText();
        if(text.isEmpty()) {
            return;
        }

        try {
            int width = getWidth();
            int height = getHeight();

            setSize(width, height);
            FontMetrics fm = getFontMetrics(getFont());
            int lineHeight = fm.getHeight();
            int i = text.length();
            double dy;
            do {
                --i;
                Rectangle rect = modelToView(i);
                if(rect == null) {
                    return;
                }

                dy = rect.y + rect.height - height;
                if(rect.x + rect.width + fm.stringWidth(text.substring(i, i + 1)) + /* magic? */ 2 > width) {
                    dy += lineHeight;
                }
            } while (dy > 0);
            if(i + 1 < text.length()) {
                String trimmed = text.substring(0, i + 1);
                trimmed = trimmed.replaceFirst("\\s*$", "").replaceFirst(".{3}$", ""); // room for ellipsis
                super.setText(trimmed + "...");
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
