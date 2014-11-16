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

import com.hp.alm.ali.idea.impl.SpellCheckerManager;
import com.hp.alm.ali.idea.spellcheck.SpellCheckTokenizer;
import com.intellij.openapi.project.Project;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class SpellCheckDocumentListener implements DocumentListener, ActionListener {

    private static final SquiggleLine zigZagPainter = new SquiggleLine(Color.RED);

    private final JTextComponent comp;
    private final SpellCheckerManager spellCheckerManager;
    private LinkedList<Modification> modifications = new LinkedList<Modification>();
    private Timer timer;

    public SpellCheckDocumentListener(Project project, JTextComponent comp) {
        this.comp = comp;

        spellCheckerManager = project.getComponent(SpellCheckerManager.class);
        timer = new Timer(500, this);
        timer.setRepeats(false);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        addDirty(e.getOffset(), e.getLength());
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        addDirty(e.getOffset(), e.getLength());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    private void addDirty(int offset, int length) {
        clear(offset, length);
        timer.restart();
        if (!modifications.isEmpty()) {
            Modification last = modifications.getLast();
            if (last.getEndOffset() == offset) {
                last.append(length);
                return;
            }
            if (last.offset == offset + length) {
                last.prepend(length);
                return;
            }
        }
        modifications.add(new Modification(offset, length));
    }

    private void check() {
        LinkedHashSet<Element> paragraphs = new LinkedHashSet<Element>();
        Document document = comp.getDocument();
        for (Modification modification : modifications) {
            for (int i = modification.offset; i < modification.offset + modification.length; ) {
                Element element = ((AbstractDocument)document).getParagraphElement(i);
                paragraphs.add(element);
                if (element.getEndOffset() > i) {
                    i = element.getEndOffset();
                } else {
                    i++;
                }
            }
        }
        modifications.clear();
        for (Element element: paragraphs) {
            check(element);
        }
    }

    private void clear(int start, int length) {
        int end = start + length;
        Highlighter highlighter = comp.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = highlights.length; --i >= 0; ) {
            Highlighter.Highlight highlight = highlights[i];
            if (highlight.getStartOffset() < end && highlight.getEndOffset() >= start) {
                if(highlight.getPainter() == zigZagPainter) {
                    highlighter.removeHighlight(highlight);
                }
            }
        }
    }

    private void check(Element element) {
        Highlighter highlighter = comp.getHighlighter();

        try {
            int start = element.getStartOffset();
            int end = element.getEndOffset();

            clear(start, end);

            String elementText = element.getDocument().getText(start, end - start);
            SpellCheckTokenizer tokenizer = new SpellCheckTokenizer(elementText);
            while (tokenizer.hasMoreTokens()) {
                SpellCheckTokenizer.Token token = tokenizer.nextToken();
                int startPos = start + token.getOffset();
                int endPos = startPos + token.getLength();
                if (spellCheckerManager.hasProblem(token.getWord())) {
                    highlighter.addHighlight(startPos, endPos, zigZagPainter);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        check();
    }

    private static class Modification {
        private int offset;
        private int length;

        public Modification(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public int getEndOffset() {
            return offset + length;
        }

        public void append(int length) {
            this.length += length;
        }

        public void prepend(int length) {
            this.offset -= length;
            this.length += length;
        }
    }
}
