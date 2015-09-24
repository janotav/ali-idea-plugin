// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

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

public class HREFLinkRecognizerTest extends IntellijTest {

    private HREFLinkRecognizer recognizer;

    public HREFLinkRecognizerTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() {
        recognizer = new HREFLinkRecognizer();
    }

    @After
    public void postClean() {
        getComponent(BrowserUtil.class)._restore();
    }

    @Test
    public void testRecognize() {
        LinkedList<Candidate> list = new LinkedList<Candidate>();
        String value = "<p>According to:&nbsp;<a>Development</a>&nbsp; (<a href=\"https://mycompany.com/view\">Web view</a>)</p>";
        recognizer.recognize(value, list);
        Assert.assertEquals(1, list.size());
        AbstractRecognizerTest.checkCandidate(list.get(0), null, value.indexOf("<a href"), value.indexOf(")</p>"), value.indexOf("<a href"), value.indexOf(")</p>"));
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
        Assert.assertTrue(recognizer.navigate(getProject(), "http://web.server.com/some/path"));
    }

    @Test
    public void testNavigate_negative() {
        Assert.assertFalse(recognizer.navigate(getProject(), "ftp://not.supported"));
    }
}
