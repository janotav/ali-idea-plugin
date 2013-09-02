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
import com.hp.alm.ali.idea.entity.EntityAdapter;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.progress.IndicatingInputStream;
import com.hp.alm.ali.idea.rest.RestException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

public class AttachmentServiceTest extends IntellijTest {

    public AttachmentServiceTest() {
        super(ServerVersion.AGM);
    }

    private AttachmentService attachmentService;
    private File file;

    @Before
    public void preCleanup() throws IOException {
        attachmentService = getComponent(AttachmentService.class);
        file = createFile();
    }

    @Test
    public void testCreateAttachment() throws IOException {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments", 201)
                .expectHeader("Content-Type", "application/octet-stream")
                .expectHeader("Slug", "logfile.txt")
                .content("attachmentServiceTest_attachment.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                checkAttachment(entity);
                Assert.assertEquals(EntityListener.Event.CREATE, event);
            }
        }));

        String name = attachmentService.createAttachment("logfile.txt", new IndicatingInputStream(file, null), file.length(), new EntityRef("defect", 1));
        Assert.assertEquals("logfile.txt", name);
    }

    @Test
    public void testCreateAttachment_failure() throws IOException {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments", 500)
                .responseBody("Failed");

        String name = attachmentService.createAttachment("logfile.txt", new IndicatingInputStream(file, null), file.length(), new EntityRef("defect", 1));
        Assert.assertNull(name);

        checkError("Failed");
    }

    @Test
    public void testDeleteAttachment() {
        handler.addRequest("DELETE", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 200)
                .content("attachmentServiceTest_attachment.xml");

        handler.async();
        addEntityListener(new EntityNotFound(handler, "attachment", 653, true));
        attachmentService.deleteAttachment("logfile.txt", new EntityRef("defect", 1));
    }

    @Test
    public void testDeleteAttachment_failure() {
        handler.addRequest("DELETE", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 500)
                .responseBody("Failed");

        attachmentService.deleteAttachment("logfile.txt", new EntityRef("defect", 1));
        checkError("Failed");
    }

    @Test
    public void testUpdateAttachmentProperty() {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 200)
                .content("attachmentServiceTest_attachment.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                checkAttachment(entity);
                Assert.assertEquals(EntityListener.Event.GET, event);
            }
        }));

        Entity attachment = attachmentService.updateAttachmentProperty("logfile.txt", new EntityRef("defect", 1), "description", "newValue", false);
        checkAttachment(attachment);
    }

    @Test
    public void testUpdateAttachmentProperty_fail() {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 500)
                .responseBody("Failed");

        Entity attachment = attachmentService.updateAttachmentProperty("logfile.txt", new EntityRef("defect", 1), "description", "newValue", false);
        Assert.assertNull(attachment);
        checkError("Failed");
    }

    @Test
    public void testUpdateAttachmentProperty_failSilently() {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 500)
                .responseBody("Failed");

        Entity attachment = attachmentService.updateAttachmentProperty("logfile.txt", new EntityRef("defect", 1), "description", "newValue", true);
        Assert.assertNull(attachment);
    }

    @Test
    public void testUpdateAttachmentContent() throws FileNotFoundException {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 200)
                .expectHeader("Content-Type", "application/octet-stream")
                .content("attachmentServiceTest_attachment.xml");

        handler.async();
        addEntityListener(new EntityLoaded(handler, new EntityLoaded.Listener() {
            @Override
            public void evaluate(Entity entity, EntityListener.Event event) {
                checkAttachment(entity);
                Assert.assertEquals(EntityListener.Event.GET, event);
            }
        }));

        boolean updated = attachmentService.updateAttachmentContent("logfile.txt", new EntityRef("defect", 1), new IndicatingInputStream(file, null), file.length(), false);
        Assert.assertTrue(updated);
    }

    @Test
    public void testUpdateAttachmentContent_fail() throws FileNotFoundException {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 500)
                .responseBody("Failed");

        boolean updated = attachmentService.updateAttachmentContent("logfile.txt", new EntityRef("defect", 1), new IndicatingInputStream(file, null), file.length(), false);
        Assert.assertFalse(updated);
        checkError("Failed");
    }

    @Test
    public void testUpdateAttachmentContent_failSilently() throws FileNotFoundException {
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/logfile.txt", 500)
                .responseBody("Failed");

        boolean updated = attachmentService.updateAttachmentContent("logfile.txt", new EntityRef("defect", 1), new IndicatingInputStream(file, null), file.length(), true);
        Assert.assertFalse(updated);
    }

    @Test
    public void testGetAttachmentEntity() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/my%20name?alt=application/xml", 200)
                .content("attachmentServiceTest_attachment.xml");

        Entity attachment = new Entity("attachment", 653);
        attachment.setProperty("name", "my name");
        attachment.setProperty("parent-type", "defect");
        attachment.setProperty("parent-id", "1");

        Entity entity = attachmentService.getAttachmentEntity(attachment);
        checkAttachment(entity);
    }

    @Test
    public void testGetAttachmentEntity_alternative() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/my%20name?alt=application/xml", 200)
                .content("attachmentServiceTest_attachment.xml");

        Entity entity = attachmentService.getAttachmentEntity("my name", new EntityRef("defect", 1));
        checkAttachment(entity);
    }

    @Test
    public void testGetAttachmentEntity_failure() {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects/1/attachments/my%20name?alt=application/xml", 500)
                .responseBody("Not this time");

        Entity entity = attachmentService.getAttachmentEntity("my name", new EntityRef("defect", 1));
        Assert.assertNull(entity);
        checkError("Not this time");
    }

    @Test
    public void testGetAttachmentEntity_illegal() {
        Entity defect = new Entity("defect", 1);
       try {
            attachmentService.getAttachmentEntity(defect);
            Assert.fail("should have failed");
        } catch (IllegalArgumentException e) {
        }
    }

    private void checkAttachment(Entity entity) {
        Assert.assertEquals("attachment", entity.getType());
        Assert.assertEquals(653, entity.getId());
        Assert.assertEquals("1", entity.getPropertyValue("parent-id"));
        Assert.assertEquals("defect", entity.getPropertyValue("parent-type"));
        Assert.assertEquals("7", entity.getPropertyValue("file-size"));
    }

    private File createFile() throws IOException {
        File tempFile = File.createTempFile("AttachmentServiceTest", null);
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        IOUtils.copy(new StringReader("content"), fw);
        fw.close();
        return tempFile;
    }
}
