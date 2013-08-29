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

public class JavaCompileRecognizer extends ClassRecognizer {

    // /usr/local/janotav/hudson/jobs/FreeHelloWorld/workspace/src/test/java/hello/HelloWorldTest.java:[19,27]
    private static Pattern COMPILE_PATTERN = Pattern.compile("(/[^:]+)?/(([^:]+)\\.java):\\[(\\d+),\\d+\\]");

    @Override
    public void recognize(String content, List<Candidate> candidates) {
        Matcher matcher = COMPILE_PATTERN.matcher(content);
        while(matcher.find()) {
            candidates.add(new ClassCandidate(matcher.start(), matcher.end(), matcher.start(2), matcher.end(2), matcher.group(3), matcher.group(3), Integer.valueOf(matcher.group(4)), null));
        }
    }
}
