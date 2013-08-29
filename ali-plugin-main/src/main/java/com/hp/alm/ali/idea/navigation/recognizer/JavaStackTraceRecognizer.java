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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaStackTraceRecognizer extends ClassRecognizer {

    // at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
    // at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
    // at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
    private static Pattern STACKTRACE_PATTERN = Pattern.compile("(at ([^(]+?))\\((([^)]+?)\\.java(:(\\d+))?)\\)", Pattern.DOTALL);

    @Override
    public void recognize(String content, List<Candidate> candidates) {
        Matcher matcher = STACKTRACE_PATTERN.matcher(content);
        while(matcher.find()) {
            String className;
            String signature = matcher.group(2);
            if(signature.contains("$")) {
                className = signature.substring(0, signature.indexOf("$"));
            } else {
                className = signature.substring(0, signature.lastIndexOf("."));
            }
            String line = matcher.group(6);
            candidates.add(new ClassCandidate(matcher.start(), matcher.end(), matcher.start(3), matcher.end(3), className, matcher.group(4), line == null? 0: Integer.valueOf(line), null));
        }
    }
}
