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

package com.hp.alm.ali.idea.tasks;

import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class HpAlmCommentTest {

    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", new Locale("cs_CZ"));

    @Test
    public void testExample1() {
        HpAlmComment comment = HpAlmComment.parse("<b>Administrator &lt;admin@company&gt;, 2012-03-13 12:57:26 +0100</b> Text");
        Assert.assertEquals("Administrator <admin@company>", comment.getAuthor());
        Assert.assertEquals("2012-03-13 12:57:26 +0100", format.format(comment.getDate()));
        Assert.assertEquals("Text", comment.getText());
    }

    @Test
    public void testExample2() {
        HpAlmComment comment = HpAlmComment.parse("<b>Administrator &lt;admin@company&gt;, 3/13/2012:</b> Text");
        Assert.assertEquals("Administrator <admin@company>", comment.getAuthor());
        Assert.assertEquals("2012-03-13 00:00:00 +0100", format.format(comment.getDate()));
        Assert.assertEquals("Text", comment.getText());
    }

    @Test
    public void testExample3() {
        HpAlmComment comment = HpAlmComment.parse("<b>admin, 3/13/2012:</b> Text");
        Assert.assertEquals("admin", comment.getAuthor());
        Assert.assertEquals("2012-03-13 00:00:00 +0100", format.format(comment.getDate()));
        Assert.assertEquals("Text", comment.getText());
    }

    @Test
    public void testExample4() {
        HpAlmComment comment = HpAlmComment.parse("<strong> Jack Jack , Mon Sep 17 2012:</strong> Text");
        Assert.assertEquals("Jack Jack", comment.getAuthor());
        Assert.assertEquals("2012-09-17 00:00:00 +0200", format.format(comment.getDate()));
        Assert.assertEquals("Text", comment.getText());
    }

    @Test
    public void testExample5() {
        HpAlmComment comment = HpAlmComment.parse("Something completely different");
        Assert.assertEquals("N/A", comment.getAuthor());
        Assert.assertEquals("1970-01-01 01:00:00 +0100", format.format(comment.getDate()));
        Assert.assertEquals("Something completely different", comment.getText());
    }
}
