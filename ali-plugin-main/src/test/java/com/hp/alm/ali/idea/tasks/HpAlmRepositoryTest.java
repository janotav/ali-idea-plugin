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
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskType;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HpAlmRepositoryTest extends IntellijTest {

    private AliProjectConfiguration projectConfiguration;

    public HpAlmRepositoryTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() {
        projectConfiguration = getComponent(AliProjectConfiguration.class);
    }

    @Test
    public void testGetIssues() throws Exception {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        RestInvocations.loadMetadata(handler, "requirement");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=&query={name['*what*']}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=&query={name['*what*']}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        Task[] list = repository.getIssues("what", 5, 100);
        Assert.assertEquals(1, list.length);
        checkTask(list[0]);
    }

    @Test
    public void testGetIssues_unassigned() throws Exception {
        HpAlmRepository repository = new HpAlmRepository();

        Task[] list = repository.getIssues("foo", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testGetIssues_noDefects() throws Exception {
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        RestInvocations.loadMetadata(handler, "requirement");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=&query={name['*foo*']}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        TaskConfig config = new TaskConfig("defect");
        config.setEnabled(false);
        repository.setDefect(config);

        Task[] list = repository.getIssues("foo", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testGetIssues_storedQuery() throws Exception {
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        RestInvocations.loadMetadata(handler, "requirement");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=&query={id[1]; name['*foo*']}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        TaskConfig config = new TaskConfig("defect");
        config.setEnabled(false);
        repository.setDefect(config);

        EntityQuery query = new EntityQuery("requirement");
        query.setName("fav1");
        query.setValue("id", "1");
        projectConfiguration.storeFilter("requirement", query);

        TaskConfig reqConfig = new TaskConfig("requirement");
        reqConfig.setCustomSelected(false);
        reqConfig.setStoredQuery(query.getName() + " (project)");
        repository.setRequirement(reqConfig);

        Task[] list = repository.getIssues("foo", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testGetIssues_storedQuery_missing() throws Exception {
        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        TaskConfig config = new TaskConfig("defect");
        config.setEnabled(false);
        repository.setDefect(config);

        TaskConfig reqConfig = new TaskConfig("requirement");
        reqConfig.setCustomSelected(false);
        reqConfig.setStoredQuery("fav2 (project)");
        repository.setRequirement(reqConfig);

        final MessageBusConnection connection = getProject().getMessageBus().connect();

        handler.async();
        connection.subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(@NotNull Notification notification) {
                Assert.assertEquals("HP ALM Integration", notification.getGroupId());
                Assert.assertEquals("Cannot retrieve task information from HP ALM:<br/> 'fav2 (project)' query not found", notification.getTitle());
                Assert.assertEquals(NotificationType.ERROR, notification.getType());
                connection.disconnect();
                handler.done();
            }

            @Override
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType) {

            }

            @Override
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType, boolean shouldLog) {
            }

            // needed for 13, adapter class not defined in 12.1.1
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType, boolean shouldLog, boolean shouldReadAloud) {
            }
        });

        Task[] list = repository.getIssues("foo", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testGetIssues_custom() throws Exception {
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        RestInvocations.loadMetadata(handler, "requirement");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=&query={id[2]; name['*foo*']}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        TaskConfig config = new TaskConfig("defect");
        config.setEnabled(false);
        repository.setDefect(config);

        TaskConfig reqConfig = new TaskConfig("requirement");
        EntityQuery query = new EntityQuery("requirement");
        query.setValue("id", "2");
        reqConfig.setCustomFilter(query);
        repository.setRequirement(reqConfig);

        Task[] list = repository.getIssues("foo", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testGetIssues_byId() throws Exception {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");
        RestInvocations.loadMetadata(handler, "requirement");

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=&query={name['*111*']}&order-by={}", 200)
                .content("no_entities.xml");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=actual-fix-time,attachment,in-bucket,changeset,build-closed,closing-version,closing-date,dev-comments,id,status,description,detected-by,build-detected,detected-in-rcyc,detected-in-rel,detection-version,creation-time,estimated-fix-time,extended-reference,fixed-on-date,has-change,has-linkage,has-others-linkage,last-modified,planned-closing-ver,priority,user-04,user-03,reproducible,severity,subject,name,user-02,user-01,watch-id,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[111]}&order-by={}", 200)
                .content("no_entities.xml");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=&query={name['*111*']}&order-by={}", 200)
                .content("no_entities.xml");
        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/requirements?fields=products-id,attachment,owner,no-of-blis,comments,creation-time,req-time,description,cover-status,last-modified,name,no-of-sons,req-priority,id,req-reviewed,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[111]}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        Task[] list = repository.getIssues("111", 5, 100);
        Assert.assertEquals(0, list.length);
    }

    @Test
    public void testFindTask() throws Exception {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=actual-fix-time,attachment,in-bucket,changeset,build-closed,closing-version,closing-date,dev-comments,id,status,description,detected-by,build-detected,detected-in-rcyc,detected-in-rel,detection-version,creation-time,estimated-fix-time,extended-reference,fixed-on-date,has-change,has-linkage,has-others-linkage,last-modified,planned-closing-ver,priority,user-04,user-03,reproducible,severity,subject,name,user-02,user-01,watch-id,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        Task task = repository.findTask("defect #86");
        checkTask(task);
    }

    @Test
    public void testFindTask_unassigned() throws Exception {
        HpAlmRepository repository = new HpAlmRepository();
        Task task = repository.findTask("defect #86");
        Assert.assertNull(task);
    }

    @Test
    public void testFindTask_negative() throws Exception {
        RestInvocations.loadMetadata(handler, "defect");
        RestInvocations.loadMetadata(handler, "release-backlog-item");

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=actual-fix-time,attachment,in-bucket,changeset,build-closed,closing-version,closing-date,dev-comments,id,status,description,detected-by,build-detected,detected-in-rcyc,detected-in-rel,detection-version,creation-time,estimated-fix-time,extended-reference,fixed-on-date,has-change,has-linkage,has-others-linkage,last-modified,planned-closing-ver,priority,user-04,user-03,reproducible,severity,subject,name,user-02,user-01,watch-id,release-backlog-item.product-id,release-backlog-item.owner,release-backlog-item.blocked,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.feature-id,release-backlog-item.invested,release-backlog-item.kanban-status-id,release-backlog-item.linked-entities-info,release-backlog-item.no-of-sons,release-backlog-item.kanban-parent-status-id,release-backlog-item.rank,release-backlog-item.release-id,release-backlog-item.entity-id,release-backlog-item.remaining,release-backlog-item.sprint-id,release-backlog-item.status,release-backlog-item.kan-parent-duration,release-backlog-item.story-points,release-backlog-item.kan-status-duration,release-backlog-item.team-id,release-backlog-item.theme-id,release-backlog-item.estimated,release-backlog-item.watch-id,release-backlog-item.id,product-group-id&query={id[87]}&order-by={}", 200)
                .content("no_entities.xml");

        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        repository._assignProject();

        try {
            repository.findTask("defect #87");
        } catch (RuntimeException e) {
            Assert.assertEquals("Entity not found: defect #87", e.getMessage());
        }
    }

    @Test
    public void testExtractId() {
        HpAlmRepository repository = new HpAlmRepository();
        Assert.assertEquals("defect #123", repository.extractId("defect #123"));
    }

    @Test
    public void testClone() {
        HpAlmRepository repository = new HpAlmRepository(getProject().getName(), 1);
        HpAlmRepository clone = repository.clone();
        Assert.assertEquals(repository.getId(), clone.getId());
        Assert.assertEquals(repository.getUrl(), clone.getUrl());
        Assert.assertEquals(repository.getDefect(), clone.getDefect());
        Assert.assertEquals(repository.getRequirement(), clone.getRequirement());
    }

    @Test
    public void testCompareTo() {
        HpAlmRepository repository1 = new HpAlmRepository(getProject().getName(), 1);
        HpAlmRepository repository2 = new HpAlmRepository(getProject().getName(), 2);
        Assert.assertEquals(-1, repository1.compareTo(repository2));
        Assert.assertEquals(1, repository2.compareTo(repository1));
        Assert.assertEquals(0, repository1.compareTo(repository1));
    }

    private void checkTask(Task task) {
        Assert.assertEquals("defect #86", task.getId());
        Assert.assertEquals("2012-09-03 00:00:00 +0200", HpAlmCommentTest.format.format(task.getCreated()));
        Assert.assertEquals("something", task.getDescription());
        Assert.assertEquals("somewhat", task.getSummary());
        Assert.assertEquals(TaskType.BUG, task.getType());
        Assert.assertEquals("2013-05-27 06:13:29 +0200", HpAlmCommentTest.format.format(task.getUpdated()));
        Assert.assertEquals(2, task.getComments().length);
        Comment comment1 = task.getComments()[0];
        Assert.assertEquals("Jack Jack", comment1.getAuthor());
        Assert.assertEquals("2012-09-17 00:00:00 +0200", HpAlmCommentTest.format.format(comment1.getDate()));
        Assert.assertEquals("Temporarily fixed by removing notNull validation.", comment1.getText());
        Comment comment2 = task.getComments()[1];
        Assert.assertEquals("Joe Joe", comment2.getAuthor());
        Assert.assertEquals("2012-09-18 00:00:00 +0200", HpAlmCommentTest.format.format(comment2.getDate()));
        Assert.assertEquals("Fixed in revision 127279", comment2.getText());
    }
}
