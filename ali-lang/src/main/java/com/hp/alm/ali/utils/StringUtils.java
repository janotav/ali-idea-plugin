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

package com.hp.alm.ali.utils;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: upgrade to up-to-date version of commons-lang and remove this class
public class StringUtils {

    public static final Pattern TEMPLATE_VAR = Pattern.compile("\\{(\\w+)\\}");

    public static String removeEnd(String str, String remove) {
        if (str == null || remove == null) {
            return str;
        } else if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        } else {
            return str;
        }
    }

    public static String escapeUrl(String src) {
        // first escaped character has to be %
        return src.replaceAll("%", "%25").replaceAll("\\s", "%20");
    }

    public static String removeStart(String str, String remove) {
        if (str == null || remove == null) {
            return str;
        } else if (str.startsWith(remove)) {
            return str.substring(remove.length());
        } else {
            return str;
        }
    }

    public static String expand(String template, Map<String, String> vars) {
        Matcher matcher = TEMPLATE_VAR.matcher(template);
        Set<String> varSet = new HashSet<String>();
        while (matcher.find()) {
            varSet.add(matcher.group(1));
        }
        if (!varSet.equals(vars.keySet())) {
            throw new IllegalArgumentException(MessageFormat.format("{0}!={1}", varSet, vars.keySet()));
        }
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String variable = "{" + entry.getKey() + "}";
            result = result.replace(variable, entry.getValue());
        }
        return result;
    }

    /**
     * Join strings with separator.
     * Strings are joined by exactly one separator.
     * <p/>
     * Examples:
     * joinWithSeparator("/", "a", "b")  ... "a/b"
     * joinWithSeparator("/", "a/", "/b")  ... "a/b"
     * joinWithSeparator("/", "a/", "b/")  ... "a/b/"
     * joinWithSeparator("/")  ... ""
     * joinWithSeparator("/", "a")  ... "a"
     * joinWithSeparator("/", "/a/")  ... "/a/"
     * <p/>
     * Corner cases like empty separator, empty tokens and multiple occurrence of separator at the beginning or tail of tokens are not supported (undefined result).
     *
     * @param separator
     * @param parts
     * @return
     */
    public static String joinWithSeparator(String separator, String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts.length == 0) return "";
        if (parts.length == 1) return parts[0];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                builder.append(removeEnd(part, separator)).append(separator);
            } else if (i == parts.length - 1) {
                builder.append(removeStart(part, separator));
            } else {
                builder.append(removeStart(removeEnd(part, separator), separator)).append(separator);
            }
        }
        return builder.toString();
    }
}