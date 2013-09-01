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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.IntegrationTest;
import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.TestTarget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@TestTarget(ServerVersion.AGM)
public class ApmUIServiceIntegrationTest extends IntegrationTest {

    private ApmUIService apmUIService;

    @Before
    public void init() {
        apmUIService = myFixture.getProject().getComponent(ApmUIService.class);
    }

    @Test
    public void testCreateDefectInRelease() {
        Assert.assertNotNull(apmUIService);
    }

    @Test
    public void testCreateDefectInSprint() {
        Assert.assertNotNull(apmUIService);
    }

    @Test
    public void testCreateDefectWithFeature() {
        Assert.assertNotNull(apmUIService);
    }

    @Test
    public void testCreateRequirementInRelease() {
        Assert.assertNotNull(apmUIService);
    }

    @Test
    public void testCreateRequirementInSprint() {
        Assert.assertNotNull(apmUIService);
    }

    @Test
    public void testCreateRequirementWithFeature() {
        Assert.assertNotNull(apmUIService);
    }
}