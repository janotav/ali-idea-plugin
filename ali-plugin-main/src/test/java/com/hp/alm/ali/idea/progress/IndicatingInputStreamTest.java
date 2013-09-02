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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class IndicatingInputStreamTest extends IntellijTest {

    public IndicatingInputStreamTest() {
        super(ServerVersion.AGM);
    }

    private File file;

    @Before
    public void preClean() throws IOException {
        file = File.createTempFile("test", "");
        file.deleteOnExit();

        FileWriter fw = new FileWriter(file);
        fw.write("1234\n");
        fw.write("123456789\n");
        fw.write("5678\n");
        fw.close();
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
        IndicatingInputStream iis = new IndicatingInputStream(file, new ProgressIndicatorBase() {
            @Override
            public void setFraction(final double fraction) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(fractions.removeFirst(), (Double)fraction);
                    }
                });
            }
        });
        byte[] buf = new byte[10];
        Assert.assertEquals(5, iis.read(buf, 0, 5));
        Assert.assertEquals(10, iis.read(buf));
        Assert.assertEquals('5', iis.read());
        Assert.assertEquals(4, iis.read(buf));
    }

    @Test
    public void testCancel() throws IOException {
        IndicatingInputStream iis = new IndicatingInputStream(file, new ProgressIndicatorBase() {
            @Override
            public void setFraction(final double fraction) {
                cancel();
            }
        });
        byte[] buf = new byte[5];
        Assert.assertEquals(5, iis.read(buf));
        try {
            iis.read(buf);
            Assert.fail("Should have failed");
        } catch (CanceledException e) {
            // ok
        }

        Assert.assertEquals("1234\n", new String(buf));
    }

    @Test
    public void testObservers() throws IOException {
        IndicatingInputStream iis = new IndicatingInputStream(file, null);
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        iis.addObserver(baos1);
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        iis.addObserver(baos2);
        byte[] buf = new byte[10];
        iis.read(buf);
        Assert.assertEquals("1234\n12345", baos1.toString());
        Assert.assertEquals("1234\n12345", baos2.toString());
        iis.read(buf);
        Assert.assertEquals("1234\n123456789\n5678\n", baos1.toString());
        Assert.assertEquals("1234\n123456789\n5678\n", baos2.toString());
    }
}
