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
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityServiceIntegrationTest extends IntegrationTest {

    private EntityService entityService;

    @Before
    public void init() {
        entityService = myFixture.getProject().getComponent(EntityService.class);
    }

    @Test
    public void testGetEntity() {
        Entity release = entityService.getEntity(new EntityRef("release", 1001));
        Assert.assertEquals(1001, release.getId());
    }
}
