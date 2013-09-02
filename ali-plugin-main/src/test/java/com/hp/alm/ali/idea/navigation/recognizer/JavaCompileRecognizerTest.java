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

public class JavaCompileRecognizerTest extends AbstractRecognizerTest {

    private JavaCompileRecognizer recognizer;

    @Before
    public void preClean() {
        recognizer = new JavaCompileRecognizer();
    }

    @Test
    public void testRecognize() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix /usr/src/test/java/hello/HelloWorldTest.java:[19,27] suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        checkClassCandidate(list.get(0), "HelloWorldTest", "HelloWorldTest", 19, null, value.indexOf("/"), value.indexOf(" suffix"), value.indexOf("HelloWorldTest"), value.indexOf(":"));
    }

    @Test
    public void testMultiple() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value =
                "prefix /usr/src/test/java/hello/HelloWorldTest.java:[19,27] middle\n" +
                "/var/src/other/OtherWorldTest.java:[11,27] suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(2, list.size());
        checkClassCandidate(list.get(0), "HelloWorldTest", "HelloWorldTest", 19, null, value.indexOf("/usr"), value.indexOf(" middle"), value.indexOf("HelloWorldTest"), value.indexOf(":[19,27]"));
        checkClassCandidate(list.get(1), "OtherWorldTest", "OtherWorldTest", 11, null, value.indexOf("/var"), value.indexOf(" suffix"), value.indexOf("OtherWorldTest"), value.indexOf(":[11,27]"));
    }
}
