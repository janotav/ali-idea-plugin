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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HpAlmComment extends Comment {
    private String text;
    private String author;
    private Date date;

    private HpAlmComment(String author, Date date, String text) {
        this.author = author;
        this.date = date;
        this.text = text;
    }

    public static HpAlmComment parse(String htmlFragment) {
        HpAlmComment comment = parseQcWeb(htmlFragment);
        if(comment == null) {
            comment = parseQc(htmlFragment);
        }
        if(comment == null) {
            comment = parseAgm(htmlFragment);
        }
        if(comment == null) {
            comment = new HpAlmComment(null, null, HTMLAreaField.toPlainText(htmlFragment, false));
        }
        return comment;
    }

    private static HpAlmComment parseQcWeb(String htmlFragment) {
        return parse(".*?<b>(.*?&lt;.+?&gt;), (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4})(.*)", CommentField.dateTimeFormat, htmlFragment);
    }

    private static HpAlmComment parseQc(String htmlFragment) {
        return parse(".*?<b>(.*?), (\\d{1,2}/\\d{1,2}/\\d{4}):(.*)", new SimpleDateFormat("M/d/yyyy"), htmlFragment);
    }

    private static HpAlmComment parseAgm(String htmlFragment) {
        return parse(".*?<strong>(.*?), \\w+ (\\w+ \\d+ \\d+):(.*)", new SimpleDateFormat("MMM d yyyy"), htmlFragment);
    }

    private static HpAlmComment parse(String pattern, DateFormat dateFormat, String htmlFragment) {
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = p.matcher(htmlFragment);
        if(matcher.matches()) {
            String author = matcher.group(1).replaceAll("&lt;", "<").replaceAll("&gt;", ">").trim();
            String text = HTMLAreaField.toPlainText(matcher.group(3), false).trim();
            try {
                Date date = dateFormat.parse(matcher.group(2));
                return new HpAlmComment(author, date, text);
            } catch (ParseException e) {
                return new HpAlmComment(author, null, text);
            }
        }
        return null;
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
