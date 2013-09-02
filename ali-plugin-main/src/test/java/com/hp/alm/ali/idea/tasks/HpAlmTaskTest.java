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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.tasks.TaskType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class HpAlmTaskTest extends IntellijTest {

    public HpAlmTaskTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testIsInitialized() {
        Entity entity = new Entity("defect", 1);
        entity.setProperty("name", "Name...");
        entity.setProperty("description", "Description...");
        entity.setProperty("dev-comments", "comments...");
        entity.setProperty("last-modified", "2012-12-12 12:12:12");
        entity.setProperty("creation-time", "2012-12-01");
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertTrue(task.isInitialized());
    }

    @Test
    public void testIsInitialized_complete() {
        Entity entity = new Entity("defect", 1);
        entity.setComplete(true);
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertTrue(task.isInitialized());
    }

    @Test
    public void testIsInitialized_negative() {
        // verity that checks for all required
        List<String> required = Arrays.asList("name", "description", "dev-comments", "last-modified", "creation-time");
        for(String prop: required) {
            Entity entity = new Entity("defect", 1);
            for(String key: required) {
                if(!key.equals(prop)) {
                    entity.setProperty(key, key);
                }
            }
            HpAlmTask task = new HpAlmTask(getProject(), entity);
            Assert.assertFalse(task.isInitialized());
        }
    }

    @Test
    public void testGetId() {
        Entity entity = new Entity("defect", 1);
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertEquals("defect #1", task.getId());
    }

    @Test
    public void testGetSummary() {
        Entity entity = new Entity("defect", 1);
        entity.setProperty("name", "My Name");
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertEquals("My Name", task.getSummary());
    }

    @Test
    public void testGetDescription() {
        Entity entity = new Entity("defect", 1);
        entity.setProperty("description", "<html><body>This is <b>html</b>.</body></html>");
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertEquals("This is html.", task.getDescription());
    }

    @Test
    public void testGetComments() {
        Entity entity = new Entity("defect", 1);
        // check that both separators (<b> and <strong>) work
        // there is a separate test for comment parsing
        String html = "<html><body>\n" +
                "<strong>  Joe Joe, Fri Sep 07 2012:</strong><br>\nComment1\n" +
                "<strong>________________________________________</strong><br>\n" +
                "<strong>  Jim Jim, Fri Sep 07 2012:</strong><br>\nComment2\n" +
                "<b>________________________________________</b><br>\n" +
                "<strong>  John John, Fri Sep 07 2012:</strong><br>\nComment3\n" +
                "</body></html>";
        entity.setProperty("dev-comments", html);
        HpAlmTask task = new HpAlmTask(getProject(), entity);
        Assert.assertEquals(3, task.getComments().length);
        Assert.assertEquals("Joe Joe", task.getComments()[0].getAuthor());
        Assert.assertEquals("Jim Jim", task.getComments()[1].getAuthor());
        Assert.assertEquals("John John", task.getComments()[2].getAuthor());

        // requirement uses another property
        Entity requirement = new Entity("requirement", 1);
        requirement.setProperty("comments", html);
        task = new HpAlmTask(getProject(), requirement);
        Assert.assertEquals(3, task.getComments().length);
    }

    @Test
    public void testGetType() {
        Entity defect = new Entity("defect", 1);
        HpAlmTask task = new HpAlmTask(getProject(), defect);
        Assert.assertEquals(TaskType.BUG, task.getType());

        Entity requirement = new Entity("requirement", 1);
        HpAlmTask task2 = new HpAlmTask(getProject(), requirement);
        Assert.assertEquals(TaskType.FEATURE, task2.getType());
    }

    @Test
    public void testGetUpdated() {
        Entity defect = new Entity("defect", 1);
        defect.setProperty("last-modified", "2012-12-12 12:12:12");
        HpAlmTask task = new HpAlmTask(getProject(), defect);
        Assert.assertEquals("2012-12-12 12:12:12 +0100", HpAlmCommentTest.format.format(task.getUpdated()));
    }

    @Test
    public void testGetCreated() {
        Entity defect = new Entity("defect", 1);
        defect.setProperty("creation-time", "2012-12-12");
        HpAlmTask task = new HpAlmTask(getProject(), defect);
        Assert.assertEquals("2012-12-12 00:00:00 +0100", HpAlmCommentTest.format.format(task.getCreated()));
    }

    @Test
    public void testGetCreated_requirement() {
        Entity requirement = new Entity("requirement", 1);
        requirement.setProperty("creation-time", "2012-12-12");
        requirement.setProperty("req-time", "12:12:12");
        HpAlmTask task = new HpAlmTask(getProject(), requirement);
        Assert.assertEquals("2012-12-12 12:12:12 +0100", HpAlmCommentTest.format.format(task.getCreated()));
    }

    @Test
    public void testIsClosed() {
        Entity defect = new Entity("defect", 1);
        HpAlmTask task = new HpAlmTask(getProject(), defect);

        defect.setProperty("status", "Closed");
        Assert.assertTrue(task.isClosed());

        defect.setProperty("status", "Open");
        Assert.assertFalse(task.isClosed());

        Entity requirement = new Entity("requirement", 1);
        HpAlmTask task2 = new HpAlmTask(getProject(), requirement);
        Assert.assertFalse(task2.isClosed());
    }

    @Test
    public void testIsIssue() {
        Entity defect = new Entity("defect", 1);
        HpAlmTask task = new HpAlmTask(getProject(), defect);
        Assert.assertTrue(task.isIssue());

        Entity requirement = new Entity("requirement", 1);
        HpAlmTask task2 = new HpAlmTask(getProject(), requirement);
        Assert.assertFalse(task2.isIssue());
    }

    @Test
    public void testGetIssueUrl() {
        Entity defect = new Entity("defect", 1);
        HpAlmTask task = new HpAlmTask(getProject(), defect);
        String url = task._getIssueUrl();
        Assert.assertEquals("td://project.domain.localhost:"+handler.getLocalPort()+"/qcbin/[AnyModule]?EntityType=IBug&EntityID=1&ShowDetails=Y", url);

        Entity requirement = new Entity("requirement", 2);
        HpAlmTask task2 = new HpAlmTask(getProject(), requirement );
        String url2 = task2._getIssueUrl();
        Assert.assertEquals("td://project.domain.localhost:"+handler.getLocalPort()+"/qcbin/[AnyModule]?EntityType=IRequirement&EntityID=2&ShowDetails=Y", url2);
    }

    @Test
    public void testParseDate() {
        Date date = HpAlmTask.parseDate("2012-12-12 12:12:12");
        Assert.assertEquals("2012-12-12 12:12:12 +0100", HpAlmCommentTest.format.format(date));
    }

    @Test
    public void testGetDescriptionField() {
        Assert.assertEquals("description", HpAlmTask.getDescriptionField("defect"));
        Assert.assertEquals("req-comment", HpAlmTask.getDescriptionField("requirement"));
    }
}
