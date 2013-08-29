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

package com.hp.alm.ali.idea.model.type;

import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.translate.expr.ExpressionBuilder;
import com.hp.alm.ali.idea.translate.expr.ExpressionParser;
import com.hp.alm.ali.idea.translate.expr.Node;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildDurationType implements Type, Translator, FilterResolver  {

    // e.g. 3h 10m 5s
    public static Pattern PATTERN = Pattern.compile("((\\d+)h)? ?((\\d+)m)? ?((\\d+)s)?");

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return null;
    }

    @Override
    public FilterResolver getFilterResolver() {
        return this;
    }

    @Override
    public Translator getTranslator() {
        return this;
    }

    @Override
    public String translate(String value, ValueCallback callback) {
        long time = Long.valueOf(value);

        long hours = time / 3600000;
        time -= (hours * 3600000);
        long mins = time / 60000;
        time -= mins * 60000;
        long secs = time / 1000;

        StringBuffer buf = new StringBuffer();
        if(hours > 0) {
            buf.append(hours).append("h");
        }
        if(mins > 0) {
            if(buf.length() > 0) {
                buf.append(" ");
            }
            buf.append(mins).append("m");
        }
        if(secs > 0 || buf.length() == 0) {
            if(buf.length() > 0) {
                buf.append(" ");
            }
            buf.append(secs).append("s");
        }

        return buf.toString();
    }

    @Override
    public String resolveDisplayValue(String value, ValueCallback onValue) {
        onValue.value(value);
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        Node node = ExpressionParser.parse(value);
        translate(node);
        return ExpressionBuilder.build(node);
    }

    private void translate(Node node) {
        long value;
        switch (node.type) {
            case VALUE:
                value = resolveValue(node.value);
                node.left = new Node(com.hp.alm.ali.idea.translate.expr.Type.GTE, new Node(String.valueOf(value)));
                node.right = new Node(com.hp.alm.ali.idea.translate.expr.Type.LT, new Node(String.valueOf(value + 1000)));
                node.type = com.hp.alm.ali.idea.translate.expr.Type.AND;
                node.value = null;
                return;

            case EQ:
                value = resolveValue(node.left.value);
                node.left = new Node(com.hp.alm.ali.idea.translate.expr.Type.GTE, new Node(String.valueOf(value)));
                node.right = new Node(com.hp.alm.ali.idea.translate.expr.Type.LTE, new Node(String.valueOf(value + 999)));
                node.type = com.hp.alm.ali.idea.translate.expr.Type.AND;
                node.value = null;
                return;

            case LTE:
            case GT:
                value = resolveValue(node.left.value);
                node.left.value = String.valueOf(value + 999);
                return;

            case DIFFERS:
                value = resolveValue(node.left.value);
                node.left = new Node(com.hp.alm.ali.idea.translate.expr.Type.LT, new Node(String.valueOf(value)));
                node.right = new Node(com.hp.alm.ali.idea.translate.expr.Type.GTE, new Node(String.valueOf(value + 1000)));
                node.type = com.hp.alm.ali.idea.translate.expr.Type.OR;
                node.value = null;
                return;

            case LT:
            case GTE:
                value = resolveValue(node.left.value);
                node.left.value = String.valueOf(value);
                return;
        }

        if(node.left != null) {
            translate(node.left);
        }
        if(node.right != null) {
            translate(node.right);
        }
    }

    private long resolveValue(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if(matcher.matches()) {
            long result = 0;
            if(matcher.group(2) != null) {
                result += (long)3600000 * Integer.valueOf(matcher.group(2));
            }
            if(matcher.group(4) != null) {
                result += (long)60000 * Integer.valueOf(matcher.group(4));
            }
            if(matcher.group(6) != null) {
                result += 1000 * Integer.valueOf(matcher.group(6));
            }
            return result;
        } else {
            throw new IllegalArgumentException("Bad duration format: "+value);
        }
    }
}
