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

package com.hp.alm.ali.idea.navigation.recognizer;

import com.hp.alm.ali.idea.navigation.Candidate;
import com.hp.alm.ali.idea.navigation.Recognizer;
import com.hp.alm.ali.idea.util.BrowserUtil;
import com.intellij.openapi.project.Project;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

public class HREFLinkRecognizer implements Recognizer {

    @Override
    public void recognize(String content, final List<Candidate> candidates) {
        try {
            new ParserDelegator().parse(new StringReader(content), new ParserCallback(candidates), true);
        } catch (IOException e) {
            // best-effort, don't panic
        }
    }

    @Override
    public boolean navigate(Project project, String hyperlink) {
        if(hyperlink.matches("https?://.*")) {
            project.getComponent(BrowserUtil.class).launchBrowser(hyperlink);
            return true;
        } else {
            return false;
        }
    }

    private static class ParserCallback extends HTMLEditorKit.ParserCallback {
        private List<Candidate> candidates;
        private int aStart;
        private String href;

        public ParserCallback(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (HTML.Tag.A.equals(t)) {
                aStart = pos;
                if (Collections.list(a.getAttributeNames()).contains(HTML.Attribute.HREF)) {
                    href = (String) a.getAttribute(HTML.Attribute.HREF);
                }
            }
        }
        public void handleEndTag(HTML.Tag t, int pos) {
            if (href != null && HTML.Tag.A.equals(t)) {
                // keep "a href" links unchanged
                candidates.add(new Candidate(aStart, pos + "</a>".length(), null));
                href = null;
            }
        }
    }
}
