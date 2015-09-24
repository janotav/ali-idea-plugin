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
import org.junit.Assert;
import org.junit.Test;

public class NavigationDecoratorTest extends IntellijTest {

    public NavigationDecoratorTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testExplode() throws InterruptedException {
        // test depends on WebLinkRecognizer
        String value = NavigationDecorator.explode(getProject(),
                "Go to http://www.company.com/ \nEnjoy!");
        Assert.assertEquals(
                "Go to <a href=\"web:http://www.company.com/\">http://www.company.com/</a> <br>Enjoy!", value);
    }

    @Test
    public void testExplodeHtml() throws InterruptedException {
        // test depends on WebLinkRecognizer
        String value = NavigationDecorator.explodeHtml(getProject(),
                "Go to http://www.company.com/ <br>\nEnjoy!");
        Assert.assertEquals(
                "Go to <a href=\"web:http://www.company.com/\">http://www.company.com/</a> <br>\nEnjoy!", value);
    }

    @Test
    public void testExplodeHtmlKeepAHref() throws InterruptedException {
        // test depends on WebLinkRecognizer and HREFLinkRecognizer
        String value = NavigationDecorator.explodeHtml(getProject(),
                "Go to http://www.company.com/ <br>\nEnjoy! <a href='http://go.com'>Regular link</a>");
        Assert.assertEquals(
                "Go to <a href=\"web:http://www.company.com/\">http://www.company.com/</a> <br>\nEnjoy! <a href='http://go.com'>Regular link</a>", value);
    }
}
