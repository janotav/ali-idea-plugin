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

public class JavaStackTraceRecognizerTest  extends AbstractRecognizerTest {

    private JavaStackTraceRecognizer recognizer;

    @Before
    public void preClean() {
        recognizer = new JavaStackTraceRecognizer();
    }

    @Test
    public void testSimpleCase() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix at org.junit.runners.ParentRunner.run(ParentRunner.java:236) suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        checkClassCandidate(list.get(0), "org.junit.runners.ParentRunner", "ParentRunner", 236, null, value.indexOf("at org"), value.indexOf(" suffix"), value.indexOf("(") + 1, value.indexOf(")"));
    }

    @Test
    public void testGeneratedMethod() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42) suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        checkClassCandidate(list.get(0), "org.junit.runners.ParentRunner", "ParentRunner", 42, null, value.indexOf("at org"), value.indexOf(" suffix"), value.indexOf("(") + 1, value.indexOf(")"));
    }

    @Test
    public void testAnonymousClass() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184) suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        checkClassCandidate(list.get(0), "org.junit.runners.ParentRunner", "ParentRunner", 184, null, value.indexOf("at org"), value.indexOf(" suffix"), value.indexOf("(") + 1, value.indexOf(")"));
    }

    @Test
    public void testMultiple() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value =
                "prefix at org.junit.runners.ParentRunner.run2(ParentRunner.java:230) suffix\n" +
                "prefix at org.junit.runners.ParentRunner.run1(ParentRunner.java:236) suffix";
        int firstLineLength = value.indexOf("prefix", 1);
        recognizer.recognize(value, list);
        Assert.assertEquals(2, list.size());
        checkClassCandidate(list.get(0), "org.junit.runners.ParentRunner", "ParentRunner", 230, null, value.indexOf("at org"), value.indexOf(" suffix"), value.indexOf("(") + 1, value.indexOf(")"));
        checkClassCandidate(list.get(1), "org.junit.runners.ParentRunner", "ParentRunner", 236, null, value.indexOf("at org") + firstLineLength, value.indexOf(" suffix") + firstLineLength, value.indexOf("(") + 1 + firstLineLength, value.indexOf(")") + firstLineLength);
    }
}
