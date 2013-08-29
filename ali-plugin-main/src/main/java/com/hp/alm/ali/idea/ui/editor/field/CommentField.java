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

package com.hp.alm.ali.idea.ui.editor.field;

import com.hp.alm.ali.idea.action.UndoAction;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentField extends BaseField {

    public static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final String HTML_END_RX = "^(.*)(</body>\\s*</html>\\s*)$";

    private JPanel commentPanel;
    private JTextPane addedComment;

    private String userName;
    private String fullName;

    public CommentField(String label, String value, String userName, String fullName) {
        super(label, false, value);

        this.userName = userName;
        this.fullName = fullName;

        addedComment = new JTextPane();
        addedComment.getDocument().addDocumentListener(new MyDocumentListener(this));
        UndoAction.installUndoRedoSupport(addedComment);
        HTMLAreaField.installNavigationShortCuts(addedComment);

        commentPanel = new JPanel(new BorderLayout());
        if(!value.isEmpty()) {
            Splitter splitter = new Splitter(true, 0.75f);
            splitter.setHonorComponentsMinimumSize(true);
            splitter.setShowDividerControls(true);
            splitter.setFirstComponent(createPane(HTMLAreaField.createTextPane(value)));
            splitter.setSecondComponent(createPane(addedComment));
            commentPanel.add(splitter, BorderLayout.CENTER);
        } else {
            commentPanel.add(createPane(addedComment), BorderLayout.CENTER);
        }
    }

    public Component getComponent() {
        return commentPanel;
    }

    public String getValue() {
        return mergeComment(getOriginalValue(), addedComment.getText(), userName, fullName);
    }

    public boolean isDisableDefaultAction() {
        return true;
    }

    @Override
    public void setValue(String value) {
        addedComment.setText(value);
    }

    private JBScrollPane createPane(Component component) {
        JBScrollPane pane = new JBScrollPane(component);
        pane.setPreferredSize(new Dimension(600, 30));
        pane.setMinimumSize(new Dimension(300, 30));
        return pane;
    }

    public boolean hasChanged() {
        return !addedComment.getText().isEmpty();
    }

    public static String mergeComment(String existingComment, String newComment, String userName, String fullName) {
        if(newComment.isEmpty()) {
            return existingComment;
        }

        if(existingComment == null) {
            existingComment = "";
        }

        String html_end = "";
        Matcher matcher = Pattern.compile(HTML_END_RX, Pattern.MULTILINE | Pattern.DOTALL).matcher(existingComment);
        if(matcher.matches()) {
            html_end = matcher.group(2);
            existingComment = matcher.replaceAll(Matcher.quoteReplacement(matcher.group(1)));
        }

        StringBuffer sb = new StringBuffer();
        if(!"".equals(html_end)) {
            // we assume there is already something, append...
            sb.append(existingComment);
            sb.append("<br><font color=\"#000080\"><b>________________________________________</b></font><br>");
        } else {
            // non-html content? start from scratch...
            sb.append("<html><body>");
            html_end = "</body></html>";
        }

        sb.append("<font color=\"#000080\"><b>");

        if (fullName != null) {
            sb.append(fullName);
            sb.append(" &lt;");
            sb.append(userName);
            sb.append("&gt;");
        } else {
            sb.append(userName);
        }
        sb.append(", ");
        sb.append(dateTimeFormat.format(new Date()));
        sb.append("</b></font><br>");

        newComment = newComment.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br>").replaceAll("\r", "").replaceAll("\t", "        ");
        sb.append(newComment);

        sb.append(html_end);
        return sb.toString();
    }
}
