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
import org.junit.Before;
import org.junit.Test;

public class MultipleItemsResolverTest extends IntellijTest {

    private MultipleItemsResolver resolver;

    public MultipleItemsResolverTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void setup() {
        resolver = new MultipleItemsResolver();
    }

    @Test
    public void testResolveDisplayValue() {
        handler.async();
        String value = resolver.resolveDisplayValue("A;B C;\"\"", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("A;B C;(no value)", value);
                    }
                });
            }
        });
        Assert.assertEquals("A;B C;(no value)", value);
    }

    @Test
    public void testToRESTQuery() {
        String value = resolver.toRESTQuery("A;B C;\"\"");
        Assert.assertEquals("\"A\" OR \"B C\" OR \"\"", value);
    }
}
