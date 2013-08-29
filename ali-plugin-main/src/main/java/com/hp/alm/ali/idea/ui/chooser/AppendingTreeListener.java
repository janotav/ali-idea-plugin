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

package com.hp.alm.ali.idea.ui.chooser;

import javax.swing.JTextField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AppendingTreeListener extends SingleTreeListener {

    private static final Pattern PATTERN = Pattern.compile("^(.*)\\^[^^]+\\^$");

    private Pattern pattern;

    public AppendingTreeListener(JTextField valueField, String entityType) {
        this(PATTERN, valueField, entityType);
    }

    public AppendingTreeListener(Pattern pattern, JTextField valueField, String entityType) {
        super(valueField, entityType);
        this.pattern = pattern;
    }

    protected void setValue(String value) {
        String current = valueField.getText();
        Matcher matcher = pattern.matcher(current);
        if(matcher.matches()) {
            valueField.setText(matcher.replaceAll("$1"+value.replaceAll("\\\\", "\\\\\\\\")));
        } else {
            if(current.isEmpty() || current.endsWith(" ")) {
                valueField.setText(current + value);
            } else {
                valueField.setText(current + " " + value);
            }
        }
    }

}
