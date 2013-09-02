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

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CustomizationServiceTest extends IntellijTest {

    private CustomizationService customizationService;

    public CustomizationServiceTest() {
        super(ServerVersion.ALM11);
    }

    @Before
    public void preClean() throws Throwable {
        customizationService = getComponent(CustomizationService.class);
        customizationService.connectedTo(ServerType.NONE);

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/customization/extensions/dev/preferences", 200)
                .content("customizationServiceTest.xml");
    }

    @Test
    public void testGetNewDefectStatus() {
        handler.async();
        String status = customizationService.getNewDefectStatus(new NonDispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("New", data);

                // served from cache
                Assert.assertEquals("New", customizationService.getNewDefectStatus(null));
            }
        });
        Assert.assertNull(status);

        // served from cache
        customizationService.getNewDefectStatus(new NonDispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("New", data);

                // served from cache
                Assert.assertEquals("New", customizationService.getNewDefectStatus(null));
            }
        });
    }

    @Test
    public void testGetNewDefectStatus_dispatch() {
        handler.async();
        String status = customizationService.getNewDefectStatus(new DispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("New", data);
                Assert.assertEquals("New", customizationService.getNewDefectStatus(null));
            }
        });
        Assert.assertNull(status);

        // served from cache
        customizationService.getNewDefectStatus(new DispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("New", data);
                Assert.assertEquals("New", customizationService.getNewDefectStatus(null));
            }
        });
    }
}
