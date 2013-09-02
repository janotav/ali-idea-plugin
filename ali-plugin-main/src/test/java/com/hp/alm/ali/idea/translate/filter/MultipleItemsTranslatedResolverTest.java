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

package com.hp.alm.ali.idea.translate.filter;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.translate.ValueCallback;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

public class MultipleItemsTranslatedResolverTest extends IntellijTest {

    public MultipleItemsTranslatedResolverTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testResolveDisplayValueSync() {
        MultipleItemsTranslatedResolver resolver = new MultipleItemsTranslatedResolver(new TranslatorSync());

        handler.async();
        String value = resolver.resolveDisplayValue("A;B C;\"\"", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("a;b c;(no value)", value);
                    }
                });
            }
        });
        Assert.assertEquals("a;b c;(no value)", value);
    }

    @Test
    public void testResolveDisplayValueAsync() {
        MultipleItemsTranslatedResolver resolver = new MultipleItemsTranslatedResolver(new TranslatorAsync());

        final LinkedList<String> expect = new LinkedList<String>();
        expect.add("Loading...");
        expect.add("a;b c;(no value)");
        handler.async(2);
        synchronized (expect) { // make sure that async callback is called later
            String value = resolver.resolveDisplayValue("A;B C;\"\"", new ValueCallback() {
                @Override
                public void value(final String value) {
                    synchronized (expect) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals(expect.removeFirst(), value);
                            }
                        });
                    }
                }
            });
            Assert.assertEquals("Loading...", value);
        }
    }

    @Test
    public void testToRESTQuery() {
        MultipleItemsTranslatedResolver resolver = new MultipleItemsTranslatedResolver(new TranslatorSync());
        String value = resolver.toRESTQuery("A;B C;\"\"");
        Assert.assertEquals("'A' OR 'B C' OR ''", value);
    }
}
