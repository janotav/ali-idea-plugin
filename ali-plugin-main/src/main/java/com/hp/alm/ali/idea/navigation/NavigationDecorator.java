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

package com.hp.alm.ali.idea.navigation;

import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.navigation.recognizer.JavaCompileRecognizer;
import com.hp.alm.ali.idea.navigation.recognizer.JavaStackTraceRecognizer;
import com.hp.alm.ali.idea.navigation.recognizer.SurefireTestRecognizer;
import com.hp.alm.ali.idea.navigation.recognizer.WebLinkRecognizer;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationDecorator {

    public static List<Recognizer> recognizers = Arrays.asList(
            new JavaStackTraceRecognizer(),
            new JavaCompileRecognizer(),
            new SurefireTestRecognizer(),
            new WebLinkRecognizer());

    public static String explode(Project project, String plain) {
        return explode(project, plain, false);
    }

    public static String explodeHtml(Project project, String html) {
        return explode(project, html, true);
    }

    private static String explode(Project project, String content, boolean sourceIsHtml) {
        StringBuffer buf = new StringBuffer();
        List<Candidate> candidates = new ArrayList<Candidate>();
        for(Recognizer recognizer: recognizers) {
            recognizer.recognize(content, candidates);
        }
        Collections.sort(candidates);
        int pos = 0;
        for(Candidate c: candidates) {
            if(c.getStart() < pos) {
                continue;
            }
            String link = c.createLink(project);
            if(link != null) {
                buf.append(toHtml(sourceIsHtml, content.substring(pos, c.getLinkStart())));
                buf.append("<a href=\"");
                buf.append(link);
                buf.append("\">");
                buf.append(toHtml(sourceIsHtml, content.substring(c.getLinkStart(), c.getLinkEnd())));
                buf.append("</a>");
                buf.append(toHtml(sourceIsHtml, content.substring(c.getLinkEnd(), c.getEnd())));
                pos = c.getEnd();
            }
        }
        buf.append(toHtml(sourceIsHtml, content.substring(pos)));
        return buf.toString();
    }

    private static String toHtml(boolean sourceIsHtml, String content) {
        if(sourceIsHtml) {
            return content;
        } else {
            return HTMLAreaField.toHtml(content, false);
        }
    }
}
