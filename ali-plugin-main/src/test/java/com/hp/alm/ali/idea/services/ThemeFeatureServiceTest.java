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
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ThemeFeatureServiceTest extends IntellijTest {

    private ThemeFeatureService themeFeatureService;

    public ThemeFeatureServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        themeFeatureService = getComponent(ThemeFeatureService.class);
        themeFeatureService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testGetFeatures() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=id,name,release-backlog-item.id,release-backlog-item.blocked,product-group-id&query={type-id[71]; product-group-id[1000]}&order-by={name[ASC]}&page-size=1000", 200)
                .content("themeFeatureServiceTest_features.xml");

        EntityList list = themeFeatureService.getFeatures();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Feature Due", list.get(0).getPropertyValue("name"));
        Assert.assertEquals("Feature Uno", list.get(1).getPropertyValue("name"));

        // from cache
        themeFeatureService.getFeatures();
    }

    @Test
    public void testGetFeatures_filter() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=id,name,release-backlog-item.id,release-backlog-item.blocked,product-group-id&query={type-id[71]; name[Uno]; product-group-id[1000]}&order-by={name[ASC]}&page-size=1000", 200)
                .content("themeFeatureServiceTest_features2.xml");

        EntityList list = themeFeatureService.getFeatures("Uno");
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Feature Uno", list.get(0).getPropertyValue("name"));
    }

    @Test
    public void testGetThemes() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=id,name,release-backlog-item.id,release-backlog-item.blocked,product-group-id&query={type-id[72]; product-group-id[1000]}&order-by={name[ASC]}&page-size=1000", 200)
                .content("themeFeatureServiceTest_themes.xml");

        EntityList list = themeFeatureService.getThemes();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Theme Due", list.get(0).getPropertyValue("name"));
        Assert.assertEquals("Theme Uno", list.get(1).getPropertyValue("name"));

        // from cache
        themeFeatureService.getThemes();
    }

    @Test
    public void testGetThemes_filter() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=id,name,release-backlog-item.id,release-backlog-item.blocked,product-group-id&query={type-id[72]; name[Uno]; product-group-id[1000]}&order-by={name[ASC]}&page-size=1000", 200)
                .content("themeFeatureServiceTest_themes2.xml");

        EntityList list = themeFeatureService.getThemes("Uno");
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Theme Uno", list.get(0).getPropertyValue("name"));
    }
}
