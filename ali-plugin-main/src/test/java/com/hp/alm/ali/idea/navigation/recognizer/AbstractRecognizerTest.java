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

public abstract class AbstractRecognizerTest {

    protected void checkClassCandidate(Candidate candidate, String className, String fileName, int line, String methodName, int start, int end, int linkStart, int linkEnd) {
        ClassCandidate cc = (ClassCandidate) candidate;
        Assert.assertEquals(className, cc.getClassName());
        Assert.assertEquals(fileName, cc.getFileName());
        Assert.assertEquals(line, cc.getLine());
        Assert.assertEquals(methodName, cc.getMethodName());
        checkCandidate(candidate, null, start, end, linkStart, linkEnd);
    }

    protected static void checkCandidate(Candidate candidate, String href, int start, int end, int linkStart, int linkEnd) {
        Assert.assertEquals(href, candidate.getHyperLink());
        Assert.assertEquals(start, candidate.getStart());
        Assert.assertEquals(end, candidate.getEnd());
        Assert.assertEquals(linkStart, candidate.getLinkStart());
        Assert.assertEquals(linkEnd, candidate.getLinkEnd());
    }
}
