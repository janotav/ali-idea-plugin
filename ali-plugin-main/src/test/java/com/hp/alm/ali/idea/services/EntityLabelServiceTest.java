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
import com.hp.alm.ali.TestTarget;
import com.hp.alm.ali.idea.MultiTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityLabelServiceTest extends MultiTest {

    private EntityLabelService entityLabelService;

    @Before
    public void preClean() throws Throwable {
        entityLabelService = getComponent(EntityLabelService.class);
        entityLabelService.connectedTo(ServerType.NONE);

        RestInvocations.loadCustomizationEntities(handler, getProject());
    }

    @Test
    public void testLoadEntityLabelAsync() {
        handler.async(2);
        entityLabelService.loadEntityLabelAsync("defect", new NonDispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("Defect", data);
            }
        });

        // from cache
        entityLabelService.loadEntityLabelAsync("defect", new DispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals("Defect", data);
            }
        });
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testLoadEntityLabelAsync_userStory() {
        testLoadEntityLabelAsync("requirement", "User Story");
    }

    // TODO: following two tests fail for ALM12: check if the metadata snapshot is correct

    @Test
    @TestTarget({ ServerVersion.ALI, ServerVersion.ALI2, ServerVersion.AGM })
    public void testLoadEntityLabelAsync_missingLabels1() {
        testLoadEntityLabelAsync("changeset", "Changeset");
    }

    @Test
    @TestTarget({ ServerVersion.ALI2, ServerVersion.AGM })
    public void testLoadEntityLabelAsync_missingLabels2() {
        testLoadEntityLabelAsync("build-instance", "Build");
        testLoadEntityLabelAsync("build-type", "Build Type");
    }

    private void testLoadEntityLabelAsync(String entityType, final String entityLabel) {
        handler.async();
        entityLabelService.loadEntityLabelAsync(entityType, new NonDispatchTestCallback<String>(handler) {
            @Override
            protected void evaluate(String data) {
                Assert.assertEquals(entityLabel, data);
            }
        });
    }

}
