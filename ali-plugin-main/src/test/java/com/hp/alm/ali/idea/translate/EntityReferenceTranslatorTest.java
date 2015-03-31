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

package com.hp.alm.ali.idea.translate;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.SimpleCache;
import com.hp.alm.ali.idea.model.Entity;
import org.junit.Assert;
import org.junit.Test;

public class EntityReferenceTranslatorTest extends IntellijTest {

    public EntityReferenceTranslatorTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testTranslate() {
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "defect", new SimpleCache());

        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=actual-fix-time,attachment,in-bucket,changeset,build-closed,closing-version,closing-date,dev-comments,id,status,description,detected-by,build-detected,detected-in-rcyc,detected-in-rel,detection-version,creation-time,estimated-fix-time,extended-reference,fixed-on-date,has-change,has-linkage,has-others-linkage,last-modified,planned-closing-ver,priority,user-04,user-03,reproducible,severity,subject,name,user-02,user-01,watch-id,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        handler.async();
        String name = translator.translate("86", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("somewhat", value);
                    }
                });
            }
        });
        Assert.assertNull(name);
    }

    @Test
    public void testTranslate_cache() {
        SimpleCache cache = new SimpleCache();
        Entity defect = new Entity("defect", 86);
        defect.setProperty("name", "yes");
        cache.add(defect);
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "defect", cache);

        String name = translator.translate("86", new ValueCallback() {
            @Override
            public void value(final String value) {
                Assert.fail("not expected");
            }
        });
        Assert.assertEquals("yes", name);
    }

    @Test
    public void testTranslate_notFound() {
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "defect", new SimpleCache());

        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=actual-fix-time,attachment,in-bucket,changeset,build-closed,closing-version,closing-date,dev-comments,id,status,description,detected-by,build-detected,detected-in-rcyc,detected-in-rel,detection-version,creation-time,estimated-fix-time,extended-reference,fixed-on-date,has-change,has-linkage,has-others-linkage,last-modified,planned-closing-ver,priority,user-04,user-03,reproducible,severity,subject,name,user-02,user-01,watch-id,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[87]}&order-by={}", 200)
                .content("no_entities.xml");

        handler.async();
        String name = translator.translate("87", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("N/A", value);
                    }
                });
            }
        });
        Assert.assertNull(name);
    }

    @Test
    public void testTranslate_cacheNotFound() {
        SimpleCache cache = new SimpleCache();
        cache.addNotFound(new EntityRef("defect", 86));
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "defect", cache);

        String name = translator.translate("86", new ValueCallback() {
            @Override
            public void value(final String value) {
                Assert.fail("not expected");
            }
        });
        Assert.assertEquals("N/A", name);
    }

    @Test
    public void testTranslate_build() {
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "build-instance", new SimpleCache());

        RestInvocations.loadMetadata(handler, "build-instance");
        RestInvocations.loadMetadata(handler, "build-type");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/build-instances?fields=product,category,id,status,build-system-url,type,test-coverage,start-date,description,duration,test-fail,running,label,vts,name,notes,number,qa-status,release,revision,sid,test-skip,status-message,test-ok,timestamp,test-success,version&query={id[4546]}&order-by={}", 200)
                .content("entity_build.xml");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/build-types?fields=category,server,id,product,default,defect-filter,description,enabled,last,vts,name,release,sid,version,product-group-id&query={id[110]}&order-by={}", 200)
                .content("entity_build_type.xml");

        handler.async();
        String name = translator.translate("4546", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("myApp #1563", value);
                    }
                });
            }
        });
        Assert.assertNull(name);
    }

    @Test
    public void testTranslate_cachedBuildTypeNotFound() {
        SimpleCache cache = new SimpleCache();
        Entity build = new Entity("build-instance", 4546);
        build.setProperty("number", "1563");
        build.setProperty("type", 110);
        cache.add(build);
        cache.addNotFound(new EntityRef("build-type", 110));
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "build-instance", cache);

        RestInvocations.loadMetadata(handler, "build-instance");

        String name = translator.translate("4546", new ValueCallback() {
            @Override
            public void value(final String value) {
                Assert.fail("not expected");
            }
        });
        Assert.assertEquals("N/A #1563", name);
    }

    @Test
    public void testTranslate_changeset() {
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "changeset", new SimpleCache());

        RestInvocations.loadMetadata(handler, "changeset");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/changesets?fields=id,date,owner,changed-file-count,has-defect-linkage,has-linkage,has-requirement-linkage,changed-line-count,description,vts,rev,status-message&query={id[941]}&order-by={}", 200)
                .content("entity_changeset.xml");

        handler.async();
        String name = translator.translate("941", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("2012-07-12 04:10:09 dev@null: Fixing something", value);
                    }
                });
            }
        });
        Assert.assertNull(name);
    }

    @Test
    public void testGetTargetType() {
        EntityReferenceTranslator translator = new EntityReferenceTranslator(getProject(), "defect", new SimpleCache());
        Assert.assertEquals("defect", translator.getTargetType());
    }
}
