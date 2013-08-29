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

public class SurefireTestRecognizer extends ClassRecognizer {

// -------------------------------------------------------
// T E S T S
// -------------------------------------------------------
// Running hello.HelloWorldTest
// Hello World!
// Tests run: 3, Failures: 1, Errors: 0, Skipped: 1, Time elapsed: 0.043 sec <<< FAILURE!
//
// Results :
//
// Failed tests:
//  testTwo(hello.HelloWorldTest)
//
// Tests run: 3, Failures: 1, Errors: 0, Skipped: 1
// [ERROR] There are test failures.

    private int context1Pos = 0;
    private static String[] CONTEXT1 = {
            "-------------------------------------------------------",
            " T E S T S",
            "-------------------------------------------------------"
    };
    private static Pattern PATTERN1 = Pattern.compile("Running (.+)");

    private int context2Pos = 0;
    private static String[] CONTEXT2 = {
            "Failed tests: ",
    };
    private static Pattern PATTERN2 = Pattern.compile(" +(\\w+)\\((.+)\\)");


    @Override
    public void recognize(String content, List<Candidate> candidates) {
        int pos = 0;
        int n;
        do {
            // read next line
            n = content.indexOf('\n', pos);
            String line;
            if(n < 0) {
                line = content.substring(pos);
            } else {
                line = content.substring(pos, n);
            }

            context1Pos = check(line, pos, context1Pos, CONTEXT1, PATTERN1, candidates, 1, -1);
            context2Pos = check(line, pos, context2Pos, CONTEXT2, PATTERN2, candidates, 2, 1);

            pos = n + 1;
        } while(n >= 0);
    }

    private int check(String line, int pos, int contextPos, String[] context, Pattern pattern, List<Candidate> candidates, int classGroup, int methodGroup) {
        if(contextPos == context.length) {
            Matcher matcher = pattern.matcher(line);
            if(matcher.matches()) {
                String className = matcher.group(classGroup);
                int dot = className.lastIndexOf('.');
                String fileName = className.substring(dot + 1);
                String methodName = methodGroup > 0? matcher.group(methodGroup): null;
                candidates.add(new ClassCandidate(pos, pos + line.length(), pos + matcher.start(1), pos + matcher.end(1), className, fileName, 0, methodName));
                return contextPos;
            } else {
                return 0;
            }
        } else if(line.equals(context[contextPos])) {
            return contextPos + 1;
        } else {
            return 0;
        }

    }
}
