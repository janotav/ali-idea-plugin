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
import com.hp.alm.ali.idea.content.devmotive.Commit;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DevMotiveServiceTest extends IntellijTest {

    private DevMotiveService devMotiveService;

    public DevMotiveServiceTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void preClean() throws Throwable {
        devMotiveService = getComponent(DevMotiveService.class);
    }

    @Test
    public void testGetRelatedEntities() throws IOException {
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/workspace/1000/ali/linked-items/commits", 200)
                .expectXmlBody(handler.getContent("devMotiveServiceTest_input.xml"))
                .content("devMotiveServiceTest_output.xml");
        Commit commit1 = new Commit(new MyFileRevision(new MyRevisionNumber("1"), "commit1", new Date(0)), "authorName1", "authorEmail1", "committerName1", "committerEmail1");
        Commit commit2 = new Commit(new MyFileRevision(new MyRevisionNumber("2"), "commit2", new Date(1000)), "authorName2", "authorEmail2", null, null);
        Commit commit3 = new Commit(new MyFileRevision(new MyRevisionNumber("3"), "commit3", new Date(1000000)), "authorName3", null, "committerName3", null);
        Commit commit4 = new Commit(new MyFileRevision(new MyRevisionNumber("4"), "commit4", new Date(1000000000)), "authorName4", "authorEmail4", "committerName4", "committerEmail4");
        Map<Commit, List<EntityRef>> result = devMotiveService.getRelatedEntities(Arrays.asList(commit1, commit2, commit3, commit4));
        Assert.assertNull(result.get(commit1));
        Assert.assertTrue(result.get(commit2).isEmpty());
        Assert.assertEquals(Collections.singletonList(new EntityRef("defect", 1)), result.get(commit3));
        Assert.assertEquals(Collections.singletonList(new EntityRef("requirement", 2)), result.get(commit4));
    }

    private static class MyFileRevision implements VcsFileRevision {

        private VcsRevisionNumber revisionNumber;
        private String message;
        private Date date;

        MyFileRevision(VcsRevisionNumber revisionNumber, String message, Date date) {
            this.revisionNumber = revisionNumber;
            this.message = message;
            this.date = date;
        }

        @Override
        public String getBranchName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RepositoryLocation getChangedRepositoryPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] loadContent() throws IOException, VcsException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getContent() throws IOException, VcsException {
            throw new UnsupportedOperationException();
        }

        @Override
        public VcsRevisionNumber getRevisionNumber() {
            return revisionNumber;
        }

        @Override
        public Date getRevisionDate() {
            return date;
        }

        @Override
        public String getAuthor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCommitMessage() {
            return message;
        }
    }

    private static class MyRevisionNumber implements VcsRevisionNumber {

        private String revision;

        public MyRevisionNumber(String revision) {
            this.revision = revision;
        }

        @Override
        public String asString() {
            return revision;
        }

        @Override
        public int compareTo(VcsRevisionNumber o) {
            return revision.compareTo(((MyRevisionNumber) o).revision);
        }
    }
}
