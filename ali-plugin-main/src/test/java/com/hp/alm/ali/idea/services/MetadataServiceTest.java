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
import com.hp.alm.ali.idea.MultiTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.io.IOException;

public class MetadataServiceTest extends MultiTest {

    private MetadataService metadataService;

    @Before
    public void preClean() throws IOException {
        metadataService = getComponent(MetadataService.class);
    }

    private void requests() throws IOException {
        RestInvocations.loadMetadata(handler, "defect");

        if(version == ServerVersion.AGM) {
            RestInvocations.loadMetadata(handler, "release-backlog-item");
        }
    }

    @Test
    public void testGetEntityMetadata() throws IOException {
        requests();

        Metadata metadata = metadataService.getEntityMetadata("defect");

        Assert.assertEquals("defect", metadata.getEntityType());
        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
        Assert.assertEquals(metadata, metadataService.getEntityMetadata("defect"));
    }

    @Test
    public void testGetCachedEntityMetadata() throws IOException {
        requests();

        // cache should be empty
        Metadata metadata = metadataService.getCachedEntityMetadata("defect");
        Assert.assertNull(metadata);

        // populate the cache
        metadataService.getEntityMetadata("defect");

        // there should be no more requests (is cached)
        metadata = metadataService.getCachedEntityMetadata("defect");
        Assert.assertEquals("defect", metadata.getEntityType());
        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
        Assert.assertEquals(metadata, metadataService.getEntityMetadata("defect"));
    }

    @Test
    public void testGetCachedFailure() throws IOException {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/non-existing-type/fields", 404);

        // populate the cache
        try {
            metadataService.getEntityMetadata("non-existing-type");
            Assert.fail("should have failed");
        } catch (Exception e) {
        }

        // there should be no more requests (is cached)
        try {
            metadataService.getEntityMetadata("non-existing-type");
            Assert.fail("should have failed");
        } catch (Exception e) {
        }
    }

    @Test
    public void testLoadEntityMetadataAsync() throws InterruptedException, IOException {
        requests();
        handler.async();

        metadataService.loadEntityMetadataAsync("defect", new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertFalse("Callback inside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("defect", metadata.getEntityType());
                        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
                        Assert.assertEquals(metadata, metadataService.getEntityMetadata("defect"));

                        final Thread t = Thread.currentThread();
                        metadataService.loadEntityMetadataAsync("defect", new MetadataService.MetadataCallback() {
                            @Override
                            public void metadataLoaded(Metadata metadata) {
                                Assert.assertEquals("Callback outside current thread although data should be cached", t, Thread.currentThread());
                            }

                            @Override
                            public void metadataFailed() {
                                Assert.fail("Should have succeeded");
                            }
                        });
                    }
                });
            }

            @Override
            public void metadataFailed() {
                handler.fail("Should have succeeded");
            }
        });
    }

    @Test
    public void testLoadEntityMetadataAsync_failure() throws InterruptedException, IOException {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/non-existing-type/fields", 404);
        handler.async();

        metadataService.loadEntityMetadataAsync("non-existing-type", new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.fail("Should have failed for wrong entity type");
            }

            @Override
            public void metadataFailed() {
                handler.done();
            }
        });
    }

    @Test
    public void testLoadEntityMetadataAsync_dispatch() throws InterruptedException, IOException {
        requests();
        handler.async();

        metadataService.loadEntityMetadataAsync("defect", new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertTrue("Callback outside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("defect", metadata.getEntityType());
                        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
                        Assert.assertEquals(metadata, metadataService.getEntityMetadata("defect"));
                    }
                });
            }

            @Override
            public void metadataFailed() {
                handler.fail("Should have succeeded");
            }
        });
    }

    @Test
    public void testDeprecatesCacheWhenConnected() throws InterruptedException, IOException {
        requests();
        handler.async();

        metadataService.loadEntityMetadataAsync("defect", new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defect", metadata.getEntityType());
                        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
                        Assert.assertNull(metadataService.getCachedEntityMetadata("defect"));
                    }
                });
            }

            @Override
            public void metadataFailed() {
                handler.fail("Should have succeeded");
            }
        });
        metadataService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testDeprecatesCacheWhenConnected_failure() throws InterruptedException, IOException {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/non-existing-type/fields", 404);
        handler.async();

        metadataService.loadEntityMetadataAsync("non-existing-type", new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(Metadata metadata) {
                handler.fail("Should have failed");
            }

            @Override
            public void metadataFailed() {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertNull(metadataService.getCachedFailure("non-existing-type"));
                    }
                });
            }
        });
        metadataService.connectedTo(ServerType.NONE);
    }

    @Test
    public void testProxyCreate() {
        Assert.assertFalse(MetadataService.Proxy.create(new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(Metadata metadata) {
            }

            @Override
            public void metadataFailed() {
            }
        }) instanceof AbstractCachingService.DispatchCallback);

        Assert.assertTrue(MetadataService.Proxy.create(new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(Metadata metadata) {
            }

            @Override
            public void metadataFailed() {
            }
        }) instanceof AbstractCachingService.DispatchCallback);
    }

    @Test
    public void testProxy() throws InterruptedException {
        final Metadata metadata = new Metadata(getProject(), "defect", false);
        MetadataService.Proxy proxy = MetadataService.Proxy.create(new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata data) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(metadata, data);
                    }
                });
            }

            @Override
            public void metadataFailed() {
                handler.done();
            }
        });

        handler.async();
        proxy.loaded(metadata);

        handler.async();
        proxy.failed();
    }

    @Test
    public void testProxy_dummy() throws InterruptedException {
        MetadataService.Proxy dummyProxy = MetadataService.Proxy.create(null);
        dummyProxy.failed();
        dummyProxy.loaded(new Metadata(getProject(), "defect", false));
    }
}
