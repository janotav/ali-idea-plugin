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

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.navigation.Candidate;
import com.hp.alm.ali.idea.util.BrowserUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

public class WebLinkRecognizerTest extends IntellijTest {

    private WebLinkRecognizer recognizer;

    public WebLinkRecognizerTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() {
        recognizer = new WebLinkRecognizer();
    }

    @After
    public void postClean() {
        getComponent(BrowserUtil.class)._restore();
    }

    @Test
    public void testRecognize() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix http://web.server.com/path suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        AbstractRecognizerTest.checkCandidate(list.get(0), "web:http://web.server.com/path", value.indexOf("http://"), value.indexOf(" suffix"), value.indexOf("http://"), value.indexOf(" suffix"));
    }

    @Test
    public void testMultiple() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "prefix http://web.server.com/path middle https://web.server.com/path2 suffix";
        recognizer.recognize(value, list);
        Assert.assertEquals(2, list.size());
        AbstractRecognizerTest.checkCandidate(list.get(0), "web:http://web.server.com/path", value.indexOf("http://"), value.indexOf(" middle"), value.indexOf("http://"), value.indexOf(" middle"));
        AbstractRecognizerTest.checkCandidate(list.get(1), "web:https://web.server.com/path2", value.indexOf("https://"), value.indexOf(" suffix"), value.indexOf("https://"), value.indexOf(" suffix"));
    }

    @Test
    public void testNavigate() {
        handler.async();
        getComponent(BrowserUtil.class)._setLauncher(new BrowserUtil.Launcher() {
            @Override
            public void launchBrowser(final String url) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("http://web.server.com/some/path", url);
                    }
                });
            }
        });
        Assert.assertTrue(recognizer.navigate(getProject(), "web:http://web.server.com/some%2Fpath"));
    }

    @Test
    public void testNavigate_negative() {
        Assert.assertFalse(recognizer.navigate(getProject(), ""));
    }
}
