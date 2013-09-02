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
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.rest.ServerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.io.IOException;

public class MetadataSimpleServiceTest extends MultiTest {

    private MetadataSimpleService metadataSimpleService;

    @Before
    public void preClean() throws IOException {
        metadataSimpleService = getComponent(MetadataSimpleService.class);
        metadataSimpleService.connectedTo(ServerType.NONE);
    }

    private void requests() throws IOException {
        RestInvocations.loadMetadata(handler, "defect");
    }

    @Test
    public void testGetEntityMetadata() throws IOException {
        requests();

        Metadata metadata = metadataSimpleService.getEntityMetadata("defect");

        Assert.assertEquals("defect", metadata.getEntityType());
        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
        Assert.assertEquals(metadata, metadataSimpleService.getEntityMetadata("defect"));
    }

    @Test
    public void testGetEntityMetadataAsync() throws InterruptedException, IOException {
        requests();
        handler.async();

        metadataSimpleService.getEntityMetadataAsync("defect", new MetadataService.MetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertFalse("Callback inside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("defect", metadata.getEntityType());
                        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
                        Assert.assertEquals(metadata, metadataSimpleService.getEntityMetadata("defect"));

                        final Thread t = Thread.currentThread();
                        metadataSimpleService.getEntityMetadataAsync("defect", new MetadataService.MetadataCallback() {
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
                Assert.fail("Should have succeeded");
            }
        });
    }

    @Test
    public void testGetEntityMetadataAsync_failure() throws InterruptedException, IOException {
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/customization/entities/non-existing-type/fields", 404);
        handler.async();

        metadataSimpleService.getEntityMetadataAsync("non-existing-type", new MetadataService.MetadataCallback() {
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
    public void testGetEntityMetadataAsync_dispatch() throws InterruptedException, IOException {
        requests();
        handler.async();

        metadataSimpleService.getEntityMetadataAsync("defect", new MetadataService.DispatchMetadataCallback() {
            @Override
            public void metadataLoaded(final Metadata metadata) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertTrue("Callback outside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("defect", metadata.getEntityType());
                        Assert.assertEquals(Integer.class, metadata.getField("id").getClazz());
                        Assert.assertEquals(metadata, metadataSimpleService.getEntityMetadata("defect"));
                    }
                });
            }

            @Override
            public void metadataFailed() {
                handler.fail("Should have succeeded");
            }
        });
    }
}

