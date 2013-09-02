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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

public class SurefireTestRecognizerTest extends AbstractRecognizerTest {

    private SurefireTestRecognizer recognizer;

    @Before
    public void preClean() {
        recognizer = new SurefireTestRecognizer();
    }

    @Test
    public void testRecognize() {
        String value =
            "-------------------------------------------------------\n" +
            "T E S T S\n" +
            "-------------------------------------------------------\n" +
            "Running hello.HelloWorldTest\n" +
            "Hello World!\n" +
            "Tests run: 4, Failures: 2, Errors: 0, Skipped: 1, Time elapsed: 0.043 sec <<< FAILURE!\n" +
            "\n" +
            "Results :\n" +
            "\n" +
            "Failed tests: \n" +
            "  testTwo(hello.HelloWorldTest)\n" +
            "  testThree(hello.HelloWorldTest)\n" +
            "\n" +
            "Tests run: 4, Failures: 2, Errors: 0, Skipped: 1\n" +
            "[ERROR] There are test failures.\n";

        LinkedList<Candidate> list = new LinkedList<Candidate>();
        recognizer.recognize(value, list);
        Assert.assertEquals(2, list.size());
        checkClassCandidate(list.get(0), "hello.HelloWorldTest", "HelloWorldTest", 0, "testTwo", 278, 309, 280, 287);
        checkClassCandidate(list.get(1), "hello.HelloWorldTest", "HelloWorldTest", 0, "testThree", 310, 343, 312, 321);
    }
}
