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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.intellij.tasks.Comment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HpAlmComment extends Comment {
    private String text;
    private String author;
    private Date date;

    public HpAlmComment(String htmlFragment) {
        // QCWeb comment example:
        //   <b>Administrator &lt;admin@company&gt;, 2012-03-13 12:57:26 +0100</b>
        // QC comment example:
        //   <b>Administrator &lt;admin@company&gt;, 3/13/2012:</b>
        // QC comment example:
        //   <b>admin, 3/13/2012:</b>

        Pattern qcWebAuthor = Pattern.compile(".*?<b>(.*?&lt;.+?&gt;), (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4})(.*)", Pattern.DOTALL);
        Matcher matcher = qcWebAuthor.matcher(htmlFragment);
        if(matcher.matches()) {
            author = matcher.group(1).replaceAll("&lt;", "<").replaceAll("&gt;", ">");
            text = HTMLAreaField.toPlainText(matcher.group(3), false);
            try {
                date = CommentField.dateTimeFormat.parse(matcher.group(2));
            } catch (ParseException e) {
                // this is best effort
            }
        } else {
            Pattern qcAuthor = Pattern.compile(".*?<b>(.*?), (\\d{1,2}/\\d{1,2}/\\d{4}):(.*)", Pattern.DOTALL);
            matcher = qcAuthor.matcher(htmlFragment);
            if(matcher.matches()) {
                author = matcher.group(1).replaceAll("&lt;", "<").replaceAll("&gt;", ">");
                text = HTMLAreaField.toPlainText(matcher.group(3), false);
                try {
                    date = new SimpleDateFormat("M/d/yyyy").parse(matcher.group(2));
                } catch (ParseException e) {
                    // this is best effort
                }
            } else {
                // fallback
                text = HTMLAreaField.toPlainText(htmlFragment, false);
            }
        }
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author == null? "N/A": author;
    }

    public Date getDate() {
        return date == null? new Date(0): date;
    }
}
