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

package com.hp.alm.ali.idea.navigation;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.util.BrowserUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.event.HyperlinkEvent;

public class NavigationListenerTest extends IntellijTest {

    public NavigationListenerTest() {
        super(ServerVersion.AGM);
    }

    @After
    public void postClean() {
        getComponent(BrowserUtil.class)._restore();
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
                        Assert.assertEquals("http://localhost", url);
                    }
                });
            }
        });
        // test depends on WebLinkRecognizer
        new NavigationListener(getProject()).hyperlinkUpdate(new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, null, "web:http://localhost"));
    }
}
