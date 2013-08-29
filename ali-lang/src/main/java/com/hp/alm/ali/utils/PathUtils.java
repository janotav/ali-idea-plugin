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

public class PathUtils {

    /**
     * Join path segments with separator.
     * <p>
     *
     * If path segment already contains separator on either side of the connecting point it is not duplicated.
     * <p>
     *
     * Examples:
     * pathJoin("/", "a", "b")  ... "a/b"
     * pathJoin("/", "a/", "/b")  ... "a/b"
     * pathJoin("/", "a/", "b/")  ... "a/b/"
     * pathJoin("/")  ... ""
     * pathJoin("/", "a")  ... "a"
     * pathJoin("/", "/a/")  ... "/a/"
     *
     * @param separator segment separator
     * @param parts path segments
     * @return resulting path
     */
    public static String pathJoin(String separator, String ... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts.length == 0) return "";
        if (parts.length == 1) return parts[0];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                builder.append(org.apache.commons.lang.StringUtils.removeEnd(part, separator)).append(separator);
            } else if (i == parts.length - 1) {
                builder.append(org.apache.commons.lang.StringUtils.removeStart(part, separator));
            } else {
                builder.append(org.apache.commons.lang.StringUtils.removeStart(org.apache.commons.lang.StringUtils.removeEnd(part, separator), separator)).append(separator);
            }
        }
        return builder.toString();
    }
}