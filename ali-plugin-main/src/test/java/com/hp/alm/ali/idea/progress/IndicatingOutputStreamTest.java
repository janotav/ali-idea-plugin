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

package com.hp.alm.ali.idea.progress;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class IndicatingOutputStreamTest extends IntellijTest {

    public IndicatingOutputStreamTest() {
        super(ServerVersion.AGM);
    }

    private File file;

    @Before
    public void preClean() throws IOException {
        file = File.createTempFile("test", "");
        file.deleteOnExit();
    }

    @After
    public void postClean() {
        file.delete();
    }

    @Test
    public void testReporting() throws IOException {
        final LinkedList<Double> fractions = new LinkedList<Double>();
        fractions.add(0.25);
        fractions.add(0.75);
        fractions.add(0.80);
        fractions.add(1.0);

        handler.async(4);
        IndicatingOutputStream ios = new IndicatingOutputStream(file, 20, new ProgressIndicatorBase() {
            @Override
            public void setFraction(final double fraction) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(fractions.removeFirst(), (Double) fraction);
                    }
                });
            }
        });
        byte[] buf0 = "1234\n".getBytes();
        byte[] buf1 = "123456789\n".getBytes();
        byte[] buf2 = "678\n".getBytes();

        ios.write(buf0);
        ios.write(buf1);
        ios.write('5');
        ios.write(buf2);
        ios.close();

        Assert.assertEquals("1234\n123456789\n5678\n", FileUtils.readFileToString(file));
    }

    @Test
    public void testCancel() throws IOException {
        IndicatingOutputStream ios = new IndicatingOutputStream(file, 20, new ProgressIndicatorBase() {
            @Override
            public void setFraction(final double fraction) {
                cancel();
            }
        });
        ios.write("1234\n".getBytes());
        try {
            ios.write("123456789\n".getBytes());
            Assert.fail("Should have failed");
        } catch (CanceledException e) {
            // ok
        }

        Assert.assertEquals("1234\n", FileUtils.readFileToString(file));
    }

    @Test
    public void testObservers() throws IOException {
        IndicatingOutputStream ios = new IndicatingOutputStream(file, 20, null);
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ios.addObserver(baos1);
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        ios.addObserver(baos2);
        ios.write("1234\n12345".getBytes());
        Assert.assertEquals("1234\n12345", baos1.toString());
        Assert.assertEquals("1234\n12345", baos2.toString());
        ios.write("6789\n5678\n".getBytes());
        Assert.assertEquals("1234\n123456789\n5678\n", baos1.toString());
        Assert.assertEquals("1234\n123456789\n5678\n", baos2.toString());

        Assert.assertEquals("1234\n123456789\n5678\n", FileUtils.readFileToString(file));
    }
}
