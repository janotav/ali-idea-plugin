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

import com.hp.alm.ali.idea.MultiTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.RequirementTypeList;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class RequirementTypeServiceTest extends MultiTest {

    private RequirementTypeService requirementTypeService;

    @Before
    public void preClean() throws IOException {
        requirementTypeService = getComponent(RequirementTypeService.class);
        requirementTypeService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testGetRequirementTypes() {
        RestInvocations.loadRequirementTypes(handler);

        RequirementTypeList types = requirementTypeService.getRequirementTypes();
        checkTypes(types);

        // served from cache
        requirementTypeService.getRequirementTypes();
    }

    @Test
    public void testTryRequirementTypes() {
        RequirementTypeList types = requirementTypeService.tryRequirementTypes();
        Assert.assertNull(types);

        RestInvocations.loadRequirementTypes(handler);
        requirementTypeService.getRequirementTypes();

        types = requirementTypeService.tryRequirementTypes();
        checkTypes(types);
    }

    @Test
    public void testLoadRequirementTypeList() throws InterruptedException {
        RestInvocations.loadRequirementTypes(handler);
        handler.async();
        requirementTypeService.loadRequirementTypeListAsync(new NonDispatchTestCallback<RequirementTypeList>(handler) {
            @Override
            public void evaluate(RequirementTypeList types) {
                checkTypes(types);
            }
        });
    }

    @Test
    public void testLoadRequirementTypeList_dispatch() throws InterruptedException {
        RestInvocations.loadRequirementTypes(handler);
        handler.async();
        requirementTypeService.loadRequirementTypeListAsync(new DispatchTestCallback<RequirementTypeList>(handler) {
            @Override
            public void evaluate(RequirementTypeList types) {
                checkTypes(types);
            }
        });
    }

    private void checkTypes(RequirementTypeList types) {
        List<String> names = new LinkedList<String>();
        for(Entity entity: types) {
            names.add(entity.getPropertyValue("name"));
        }

        switch (version) {
            case AGM:
                Assert.assertEquals(Arrays.asList("Theme", "Folder", "User Story", "Group Story", "Feature"), names);
                break;

            case ALI11_5:
            case ALI12:
                Assert.assertEquals(Arrays.asList("Undefined", "Folder", "Group", "Functional", "Business", "Testing", "Performance", "Business Model"), names);
                break;

            default:
                Assert.assertEquals(Arrays.asList("user story", "Undefined", "Folder", "Group", "Functional", "Business", "Testing", "Performance", "Business Model"), names);
        }
    }
}
