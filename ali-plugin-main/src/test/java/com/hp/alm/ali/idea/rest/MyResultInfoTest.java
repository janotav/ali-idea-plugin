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

package com.hp.alm.ali.idea.rest;

import com.hp.alm.ali.idea.progress.IndicatingOutputStream;
import com.hp.alm.ali.rest.client.ResultInfo;
import com.intellij.mock.MockProgressIndicator;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MyResultInfoTest {

    @Test
    public void testCopyFrom() throws IOException {
        ResultInfo result = ResultInfo.create(new ByteArrayOutputStream());
        result.setLocation("location");
        result.getHeaders().put("a", "b");
        result.getHeaders().put("c", "d");

        MyResultInfo info = new MyResultInfo();
        info.copyFrom(result);
        Assert.assertEquals(result.getHeaders(), info.getHeaders());
        Assert.assertEquals(result.getLocation(), info.getLocation());
    }

    @Test
    public void testGetBodyAsString() throws IOException {
        MyResultInfo info = new MyResultInfo();
        ResultInfo result = ResultInfo.create(info.getOutputStream());
        result.getBodyStream().write("foo".getBytes());
        Assert.assertEquals("foo", info.getBodyAsString());
    }

    @Test
    public void testGetBodyAsStream() throws IOException {
        MyResultInfo info = new MyResultInfo();
        ResultInfo result = ResultInfo.create(info.getOutputStream());
        result.getBodyStream().write("foo".getBytes());
        Assert.assertEquals("foo", IOUtils.toString(info.getBodyAsStream()));
    }

    @Test
    public void testIndicatingOutputStream() throws IOException {
        File file = File.createTempFile("test", "");
        file.deleteOnExit();
        MyResultInfo info = new MyResultInfo(new IndicatingOutputStream(file, 6, new MockProgressIndicator()));
        ResultInfo result = ResultInfo.create(info.getOutputStream());
        result.getBodyStream().write("foo".getBytes());
        Assert.assertEquals("foo", info.getBodyAsString());
        result.getBodyStream().write("bar".getBytes());
        Assert.assertEquals("foobar", info.getBodyAsString());
    }
}
