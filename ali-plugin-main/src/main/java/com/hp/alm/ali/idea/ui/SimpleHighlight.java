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

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;

public class SimpleHighlight {

    private DefaultHighlighter highlighter;
    private DefaultHighlighter.DefaultHighlightPainter painter;
    private  JTextComponent textComponent;

    public SimpleHighlight(JTextComponent textComponent) {
        this.textComponent = textComponent;
        highlighter = new DefaultHighlighter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(0xFF, 0x99, 0x33));

        textComponent.setHighlighter(highlighter);
    }

    public void setFilter(String filter) {
        highlighter.removeAllHighlights();
        if(filter == null || filter.isEmpty()) {
            return;
        }
        String text = textComponent.getText().toLowerCase();
        String str = filter.toLowerCase();
        int p = 0;
        while(true) {
            p = text.indexOf(str, p);
            if(p < 0) {
                break;
            }
            try {
                highlighter.addHighlight(p, p + str.length(), painter);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            p++;
        }
    }
}
