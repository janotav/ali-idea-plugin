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
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ProjectListServiceTest extends MultiTest {

    private ProjectListService projectListService;

    @Before
    public void preClean() throws IOException {
        projectListService = getComponent(ProjectListService.class);
        projectListService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testGetProjectList() {
        RestInvocations.loadProjectLists(handler, "defect");

        Field field = new Field("reproducible", "Reproducible");
        field.setListId(1);
        List<String> list = projectListService.getProjectList("defect", field);

        switch (version) {
            case AGM:
                Assert.assertEquals(Arrays.asList("Y", "N"), list);
                break;

            default:
                Assert.assertEquals(Arrays.asList("N", "Y"), list);
        }

        // second request served from cache
        projectListService.getProjectList("defect", field);
    }

    @Test
    public void testGetProjectList_relatedType() {
        RestInvocations.loadProjectLists(handler, "defect");

        Field field = new Field("defect.reproducible", "Reproducible");
        field.setListId(1);
        List<String> list = projectListService.getProjectList("requirement", field);

        switch (version) {
            case AGM:
                Assert.assertEquals(Arrays.asList("Y", "N"), list);
                break;

            default:
                Assert.assertEquals(Arrays.asList("N", "Y"), list);
        }
    }

    @Test
    public void testGetProjectList_nonExisting() {
        RestInvocations.loadProjectLists(handler, "defect");

        Field field = new Field("non-existing", "Non-existing");
        field.setListId(10000);
        List<String> list = projectListService.getProjectList("defect", field);
        Assert.assertTrue(list.isEmpty());
    }
}
