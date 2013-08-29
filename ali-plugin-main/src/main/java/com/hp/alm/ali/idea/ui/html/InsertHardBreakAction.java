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

package com.hp.alm.ali.idea.ui.html;

import javax.swing.JEditorPane;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.event.ActionEvent;

public class InsertHardBreakAction extends HTMLEditorKit.InsertHTMLTextAction {

    public InsertHardBreakAction() {
	    super("InsertHardBreak", "<br>", null, HTML.Tag.BR, null, null);
	}

    public void actionPerformed(ActionEvent event) {
        JEditorPane editor = getEditor(event);
        if (editor != null) {
            try {
                HTMLDocument doc = getHTMLDocument(editor);
                int offset = editor.getSelectionStart();
                Element paragraph = doc.getParagraphElement(offset);
                if (paragraph != null && "p-implied".equals(paragraph.getName())) {
                    // when editing html without paragraphs, we have to take care of inserting hard beaks
                    getHTMLEditorKit(editor).insertHTML(doc, offset, "<br>", 0, 0, HTML.Tag.BR);
                    ((DefaultCaret)editor.getCaret()).setDot(editor.getSelectionEnd(), Position.Bias.Forward);
                } else {
                    // when editing html with paragraphs, swing creates paragraphs for every break by itself
                    new DefaultEditorKit.InsertBreakAction().actionPerformed(event);
                }
           } catch (Exception ex) {
                // not an html document
                new DefaultEditorKit.InsertBreakAction().actionPerformed(event);
            }
        }
    }
}

