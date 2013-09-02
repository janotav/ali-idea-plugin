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
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SortOrder;
import java.io.IOException;
import java.util.List;

public class FavoritesServiceTest extends IntellijTest {

    private FavoritesService favoritesService;
    AliProjectConfiguration projectConfiguration;
    private AliConfiguration configuration;

    @Before
    public void preClean() throws IOException {
        favoritesService = getComponent(FavoritesService.class);
        projectConfiguration = getComponent(AliProjectConfiguration.class);
        configuration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
    }

    public FavoritesServiceTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testGetFavorite() {
        RestInvocations.loadMetadata(handler, "favorite");
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/favorites?fields=&query={id[1014]}&order-by={}", 200)
                .content("favoritesServiceTest_favorite.xml");

        EntityQuery query = favoritesService.getFavorite(1014, "defect");
        checkQuery(query);
    }

    @Test
    public void testGetStoredQuery_server() {
        RestInvocations.loadMetadata(handler, "favorite");
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/favorites?fields=&query={id[1014]}&order-by={}", 200)
                .content("favoritesServiceTest_favorite.xml");

        EntityQuery query = favoritesService.getStoredQuery("defect", "1014: blockers (ALM)");
        checkQuery(query);
    }

    @Test
    public void testGetStoredQuery_serverInvalid() {
        RestInvocations.loadMetadata(handler, "favorite");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/favorites?fields=&query={id[1015]}&order-by={}", 200)
                .content("no_entities.xml");

        EntityQuery query = favoritesService.getStoredQuery("defect", "1015: something (ALM)");
        Assert.assertNull(query);
    }

    @Test
    public void testGetStoredQuery_stored() {
        addStoredQuery();

        EntityQuery query = favoritesService.getStoredQuery("defect", "test_query (project)");
        Assert.assertEquals("1", query.getValue("id"));
    }

    @Test
    public void testGetStoredQuery_storedInvalid() {
        EntityQuery query = favoritesService.getStoredQuery("defect", "bad_test_query (project)");
        Assert.assertNull(query);
    }

    @Test
    public void testGetGlobalQuery_stored() {
        addGlobalQuery();

        EntityQuery query = favoritesService.getStoredQuery("defect", "global_test_query (global)");
        Assert.assertEquals("1", query.getValue("id"));
    }

    @Test
    public void testGetGlobalQuery_storedInvalid() {
        EntityQuery query = favoritesService.getStoredQuery("defect", "global_bad_test_query (global)");
        Assert.assertNull(query);
    }

    @Test
    public void testGetAvailableQueries() {
        addStoredQuery();
        addGlobalQuery();

        List<EntityQuery> list = favoritesService.getAvailableQueries("defect");
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("test_query (project)", list.get(0).getName());
        Assert.assertEquals("1", list.get(0).getValue("id"));
        Assert.assertEquals("global_test_query (global)", list.get(1).getName());
        Assert.assertEquals("1", list.get(1).getValue("id"));
    }

    @Test
    public void testUnresolveQuery() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        EntityQuery query = new EntityQuery("defect");
        query.setValue("status", "a OR b OR c");
        query.setValue("owner", "me or you");
        favoritesService.unresolveQuery(query);
        Assert.assertEquals("a;b;c", query.getValue("status"));
        Assert.assertEquals("me;you", query.getValue("release-backlog-item.owner"));
        Assert.assertNull(query.getValue("owner"));
    }

    private void addStoredQuery() {
        EntityQuery query = new EntityQuery("defect");
        query.setValue("id", "1");
        query.setName("test_query");
        projectConfiguration.storeFilter(query.getEntityType(), query);
    }

    private void addGlobalQuery() {
        EntityQuery query = new EntityQuery("defect");
        query.setValue("id", "1");
        query.setName("global_test_query");
        configuration.storeFilter(query.getEntityType(), query);
    }

    private void checkQuery(EntityQuery query) {
        Assert.assertNotNull(query);
        Assert.assertEquals("defect", query.getEntityType());
        // query
        Assert.assertEquals("New;Open;Reopen", query.getValue("status"));
        Assert.assertEquals("3-High;4-Very High;5-Urgent", query.getValue("severity"));
        // order
        Assert.assertEquals(1, query.getOrder().size());
        Assert.assertEquals(SortOrder.ASCENDING, query.getOrder().get("id"));
        // view
        Assert.assertEquals("{in-bucket=50, watch-id=60, id=60, name=597, status=100, owner=100, severity=100, priority=100, detected-by=100, creation-time=100}", query.getColumns().toString());
    }
}
