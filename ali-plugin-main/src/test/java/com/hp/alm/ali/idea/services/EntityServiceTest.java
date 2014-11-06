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
import com.hp.alm.ali.idea.entity.CachingEntityListener;
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SortOrder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class EntityServiceTest extends MultiTest {

    private EntityService entityService;

    @Before
    public void preClean() {
        entityService = getComponent(EntityService.class);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testGetEntity() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        Entity defect = entityService.getEntity(new EntityRef("defect", 86));
        Assert.assertEquals(86, defect.getId());

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[87]}&order-by={}", 200)
                .content("no_entities.xml");

        try{
            entityService.getEntity(new EntityRef("defect", 87));
            Assert.fail("should have failed");
        } catch (Exception e) {
            Assert.assertEquals("Entity not found: defect #87", e.getMessage());
        }
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testQuery() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=priority,release-backlog-item.id,release-backlog-item.blocked,product-group-id&query={status[\"Fixed\"]; product-group-id[1000]}&order-by={priority[DESC]}", 200)
                .content("entityServiceTest_entity.xml");

        EntityQuery query = new EntityQuery("defect");
        query.setValue("status", "Fixed");
        query.addColumn("priority", 1);
        query.addOrder("priority", SortOrder.DESCENDING);
        EntityList list = entityService.query(query);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(86, list.get(0).getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testQueryForStream() throws IOException {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=&query={status[Fixed]}&order-by={}", 200)
                .responseBody("simply content");

        EntityQuery query = new EntityQuery("defect");
        query.setValue("status", "Fixed");
        query.setPropertyResolved("status", true);
        String content = IOUtils.toString(entityService.queryForStream(query));
        Assert.assertEquals("simply content", content);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testGetEntityAsync() throws IOException {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        handler.async();
        entityService.getEntityAsync(new EntityRef("defect", 86), new EntityAdapter() {
            @Override
            public void entityLoaded(final Entity entity, final Event event) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(86, entity.getId());
                        Assert.assertEquals(Event.GET, event);
                    }
                });
            }
        });

        handler.consume();
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[87]}&order-by={}", 200)
                .content("no_entities.xml");

        handler.async();
        final EntityRef defect = new EntityRef("defect", 87);
        entityService.getEntityAsync(defect, new EntityAdapter() {
            @Override
            public void entityNotFound(final EntityRef ref, final boolean removed) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(defect, ref);
                        Assert.assertFalse(removed);
                    }
                });
            }
        });
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void refreshEntity() throws IOException {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        final EntityRef defect = new EntityRef("defect", 87);
        addEntityListener(new EntityAdapter() {
            @Override
            public void entityLoaded(final Entity entity, final Event event) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(86, entity.getId());
                        Assert.assertEquals(Event.REFRESH, event);
                    }
                });
            }
            @Override
            public void entityNotFound(final EntityRef ref, final boolean removed) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(defect, ref);
                        Assert.assertFalse(removed);
                    }
                });
            }
        });

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        handler.async();
        entityService.refreshEntity(new EntityRef("defect", 86));

        handler.consume();
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[87]}&order-by={}", 200)
                .content("no_entities.xml");

        handler.async();
        entityService.refreshEntity(defect);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testGetDefectLink_Apollo() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defect-links?fields=&query={id[2763]}&order-by={}", 200)
                .content("entityServiceTest_defectLink.xml");

        Entity link = entityService.getDefectLink(86, 2763);
        Assert.assertEquals(2763, link.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testGetDefectLink_Maya() {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 200)
                .content("entityServiceTest_defectLink.xml");

        Entity link = entityService.getDefectLink(86, 2763);
        Assert.assertEquals(2763, link.getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 200)
                .expectBody("<Entity Type=\"defect\"><Fields><Field Name=\"status\"><Value>Closed</Value></Field></Fields></Entity>")
                .content("entityServiceTest_entity.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect", 86, EntityListener.Event.GET));

        Entity defect = new Entity("defect", 86);
        defect.setProperty("priority", "1-Critical");
        defect.setProperty("status", "Closed");
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), true, false, true);
        Assert.assertEquals(86, updated.getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity_failure() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect", 86);
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), false, false, true);
        Assert.assertNull(updated);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity_silentFailure() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect", 86);
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), true, false, true);
        Assert.assertNull(updated);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity_silentReload() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 500)
                .responseBody("Failed");

        // get after failure:
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");


        Entity defect = new Entity("defect", 86);
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), true, true, false);
        Assert.assertEquals(86, updated.getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity_silentReloadFailure() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 500)
                .responseBody("Failed");

        // get after failure:
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 500)
                .responseBody("Failed again");

        Entity defect = new Entity("defect", 86);
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), false, true, false);
        Assert.assertNull(updated);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testUpdateEntity_noEvent() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86", 200)
                .expectBody("<Entity Type=\"defect\"><Fields><Field Name=\"status\"><Value>Closed</Value></Field></Fields></Entity>")
                .content("entityServiceTest_entity.xml");

        addEntityListener(new EntityAdapter() {
            @Override
            public void entityLoaded(final Entity entity, final Event event) {
                Assert.fail("Event not expected");
            }
        });

        Entity defect = new Entity("defect", 86);
        defect.setProperty("priority", "1-Critical");
        defect.setProperty("status", "Closed");
        Entity updated = entityService.updateEntity(defect, Collections.singleton("status"), false, false, false);
        Assert.assertEquals(86, updated.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testUpdateEntity_MayaLink() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 200)
                .expectBody("<defect-link><comment>Yes!</comment><first-endpoint-id>86</first-endpoint-id></defect-link>")
                .content("entityServiceTest_defectLink.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect-link", 2763, EntityListener.Event.GET));

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("comment", "Yes!");
        link.setProperty("first-endpoint-id", "86");
        Entity updated = entityService.updateEntity(link, Collections.singleton("comment"), true, false, true);
        Assert.assertEquals(2763, updated.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testUpdateEntity_MayaLink_failure() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 500)
                .responseBody("Failed");

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("first-endpoint-id", "86");
        Entity updated = entityService.updateEntity(link, Collections.singleton("comment"), false, false, false);
        Assert.assertNull(updated);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testUpdateEntity_MayaLink_silentFailure() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 500)
                .responseBody("Failed");

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("first-endpoint-id", "86");
        Entity updated = entityService.updateEntity(link, Collections.singleton("comment"), true, false, false);
        Assert.assertNull(updated);
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testUpdateEntity_MayaLink_noEvent() {
        handler.addRequest(false, "PUT", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 200)
                .expectBody("<defect-link><comment>Yes!</comment><first-endpoint-id>86</first-endpoint-id></defect-link>")
                .content("entityServiceTest_defectLink.xml");

        addEntityListener(new EntityAdapter() {
            @Override
            public void entityLoaded(final Entity entity, final Event event) {
                Assert.fail("Event not expected");
            }
        });

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("comment", "Yes!");
        link.setProperty("first-endpoint-id", "86");
        Entity updated = entityService.updateEntity(link, Collections.singleton("comment"), true, false, false);
        Assert.assertEquals(2763, updated.getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testCreateEntity() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects", 201)
                .expectBody("<Entity Type=\"defect\"><Fields><Field Name=\"status\"><Value>New</Value></Field></Fields></Entity>")
                .content("entityServiceTest_entity.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect", 86, EntityListener.Event.CREATE));

        Entity defect = new Entity("defect");
        defect.setProperty("status", "New");
        Entity created = entityService.createEntity(defect, false);
        Assert.assertEquals(86, created.getId());
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testCreateEntity_failure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect");
        Entity created = entityService.createEntity(defect, false);
        Assert.assertNull(created);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testCreateEntity_silentFailure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect");
        Entity created = entityService.createEntity(defect, true);
        Assert.assertNull(created);
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testCreateEntity_MayaLink() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links", 201)
                .expectBody("<defect-link><comment>There</comment><first-endpoint-id>86</first-endpoint-id></defect-link>")
                .content("entityServiceTest_defectLink.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect-link", 2763, EntityListener.Event.CREATE));

        Entity link = new Entity("defect-link");
        link.setProperty("first-endpoint-id", "86");
        link.setProperty("comment", "There");
        Entity created = entityService.createEntity(link, false);
        Assert.assertEquals(2763, created.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testCreateEntity_MayaLink_failure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links", 500)
                .responseBody("Failed");

        Entity link = new Entity("defect-link");
        link.setProperty("first-endpoint-id", "86");
        Entity created = entityService.createEntity(link, false);
        Assert.assertNull(created);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testCreateEntity_MayaLink_silentFailure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links", 500)
                .responseBody("Failed");

        Entity link = new Entity("defect-link");
        link.setProperty("first-endpoint-id", "86");
        Entity created = entityService.createEntity(link, true);
        Assert.assertNull(created);
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testLockEntity() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 200)
                .content("entityServiceTest_entity.xml");

        Entity defect = new Entity("defect", 86);
        Entity entity = entityService.lockEntity(defect, false);
        Assert.assertEquals(86, entity.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testLockEntity_withUpdate() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 200)
                .content("entityServiceTest_entity.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect", 86, EntityListener.Event.GET));
        testMessages.add("Item has been recently modified on the server. Local values have been updated to match the up-to-date revision.", 0);

        Entity defect = new Entity("defect", 86);
        defect.setProperty("status", "Open"); // different than obtained during lock
        Entity entity = entityService.lockEntity(defect, false);
        Assert.assertEquals(86, entity.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testLockEntity_withSilentUpdate() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 200)
                .content("entityServiceTest_entity.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, "defect", 86, EntityListener.Event.GET));

        Entity defect = new Entity("defect", 86);
        defect.setProperty("status", "Open"); // different than obtained during lock
        Entity entity = entityService.lockEntity(defect, true);
        Assert.assertEquals(86, entity.getId());
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testLockEntity_failure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect", 86);
        Entity entity = entityService.lockEntity(defect, false);
        Assert.assertNull(entity);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testLockEntity_silentFailure() {
        handler.addRequest(false, "POST", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect", 86);
        Entity entity = entityService.lockEntity(defect, true);
        Assert.assertNull(entity);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testDeleteEntity() {
        handler.addRequest(false, "DELETE", "/qcbin/rest/domains/domain/projects/project/defects/86", 200);

        handler.async();
        addEntityListener(new EntityNotFound(handler, "defect", 86, true));

        Entity defect = new Entity("defect", 86);
        boolean deleted = entityService.deleteEntity(defect);
        Assert.assertTrue(deleted);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testDeleteEntity_failure() {
        handler.addRequest(false, "DELETE", "/qcbin/rest/domains/domain/projects/project/defects/86", 500)
                .responseBody("Failed");

        Entity defect = new Entity("defect", 86);
        boolean deleted = entityService.deleteEntity(defect);
        Assert.assertFalse(deleted);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testDeleteEntity_MayaLink() {
        handler.addRequest(false, "DELETE", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 200);

        handler.async();
        addEntityListener(new EntityNotFound(handler, "defect-link", 2763, true));

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("first-endpoint-id", "86");
        boolean deleted = entityService.deleteEntity(link);
        Assert.assertTrue(deleted);
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testDeleteEntity_MayaLink_failure() {
        handler.addRequest(false, "DELETE", "/qcbin/rest/domains/domain/projects/project/defects/86/defect-links/2763", 500)
                .responseBody("Failed");

        Entity link = new Entity("defect-link", 2763);
        link.setProperty("first-endpoint-id", "86");
        boolean deleted = entityService.deleteEntity(link);
        Assert.assertFalse(deleted);
        checkError("Failed");
    }

    @Test
    @TestTarget(ServerVersion.ALM11)
    public void testUnlockEntity() {
        handler.addRequest(false, "DELETE", "/qcbin/rest/domains/domain/projects/project/defects/86/lock", 200);

        Entity defect = new Entity("defect", 86);
        entityService.unlockEntity(defect);
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testRequestCachedEntity() {
        EntityRef ref = new EntityRef("defect", 86);
        Entity entity = new Entity(ref.type, ref.id);
        entity.setProperty("status", "New");
        addEntityListener(new Cache(ref, entity));
        handler.async();
        entityService.requestCachedEntity(ref, Arrays.asList("status"), new EntityLoaded(handler, "defect", 86, EntityListener.Event.CACHE, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                Assert.assertEquals("New", entity.getPropertyValue("status"));
            }
        }));
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testRequestCachedEntity_propertyMiss() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        EntityRef ref = new EntityRef("defect", 86);
        Entity entity = new Entity(ref.type, ref.id);
        addEntityListener(new Cache(ref, entity));
        handler.async();
        entityService.requestCachedEntity(ref, Arrays.asList("status"), new EntityLoaded(handler, "defect", 86, EntityListener.Event.GET, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                Assert.assertEquals("Fixed", entity.getPropertyValue("status"));
            }
        }));
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testRequestCachedEntity_miss() {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        addEntityListener(new Cache(null, null));
        handler.async();
        entityService.requestCachedEntity(new EntityRef("defect", 86), Collections.<String>emptyList(), new EntityLoaded(handler, "defect", 86, EntityListener.Event.GET, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                Assert.assertEquals("Fixed", entity.getPropertyValue("status"));
            }
        }));
    }

    @Test
    @TestTarget(ServerVersion.AGM)
    public void testRequestCachedEntity_async() {
        EntityRef ref = new EntityRef("defect", 86);
        final Entity entity = new Entity(ref.type, ref.id);
        final String thread = Thread.currentThread().getName();
        addEntityListener(new CachingEntityListener() {
            @Override
            public Entity lookup(EntityRef ref) {
                Assert.assertNotEquals("cache must be asynchronous", Thread.currentThread().getName(), thread);
                return entity;
            }

            @Override
            public void entityLoaded(Entity entity, Event event) {
            }

            @Override
            public void entityNotFound(EntityRef ref, boolean removed) {
            }
        });
        handler.async();
        entityService.requestCachedEntity(new EntityRef("defect", 86), Collections.<String>emptyList(), new EntityLoaded(handler, "defect", 86, EntityListener.Event.CACHE));
    }

    private static class Cache implements CachingEntityListener {

        private final EntityRef key;
        private final Entity value;

        private Cache(EntityRef key, Entity value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Entity lookup(EntityRef ref) {
            if(ref.equals(key)) {
                return value;
            } else {
                return null;
            }
        }

        @Override
        public void entityLoaded(Entity entity, Event event) {
        }

        @Override
        public void entityNotFound(EntityRef ref, boolean removed) {
        }
    }
}
