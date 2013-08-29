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

import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

public class BodyLimitCaretListener implements CaretListener {

    private JTextPane html;

    public BodyLimitCaretListener(JTextPane html) {
        this.html = html;
    }

    public void caretUpdate(CaretEvent caretEvent) {
        HTMLDocument doc = (HTMLDocument)html.getDocument();
        Element body = doc.getElement(doc.getDefaultRootElement(), StyleConstants.NameAttribute, HTML.Tag.BODY);
        int startOfBody = body.getStartOffset();
        if(caretEvent.getDot() < startOfBody) {
            html.getCaret().moveDot(startOfBody);
        }
    }
}
