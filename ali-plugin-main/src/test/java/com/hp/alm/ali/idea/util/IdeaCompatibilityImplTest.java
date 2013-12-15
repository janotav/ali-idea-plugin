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

package com.hp.alm.ali.idea.util;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IdeaCompatibility;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.application.ApplicationInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IdeaCompatibilityImplTest extends IntellijTest {

    public IdeaCompatibilityImplTest() {
        super(ServerVersion.AGM);
    }

    private IdeaCompatibility ideaCompatibility;
    private int baseline;

    @Before
    public void preClean() {
        ideaCompatibility = getComponent(IdeaCompatibility.class);
        baseline = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
    }


    @Test
    public void testRegister() {
        Assert.assertTrue(ideaCompatibility.register(FooInf.class, Foo1Impl.class, baseline - 10));
        Assert.assertTrue(ideaCompatibility.register(FooInf.class, Foo2Impl.class, baseline));
        Assert.assertFalse(ideaCompatibility.register(FooInf.class, Foo1Impl.class, baseline + 10));
        Assert.assertFalse(ideaCompatibility.register(FooInf.class, Foo1Impl.class, baseline - 1));
    }

    @Test
    public void testGetComponent() {
        ideaCompatibility.register(RestService.class, RestService.class, baseline);
        Assert.assertEquals(RestService.class, ideaCompatibility.getComponent(RestService.class).getClass());
    }

    private static interface FooInf {
    }

    private static class Foo1Impl implements FooInf {
    }

    private static class Foo2Impl implements FooInf {
    }
}
