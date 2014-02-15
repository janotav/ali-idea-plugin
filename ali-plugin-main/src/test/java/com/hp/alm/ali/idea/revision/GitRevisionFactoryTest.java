/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.revision;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.testFramework.LightVirtualFile;
import git4idea.GitRevisionNumber;
import git4idea.GitVcs;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GitRevisionFactoryTest extends IntellijTest {

    private GitRevisionFactory gitRevisionFactory;
    private GitVcs gitVcs;

    public GitRevisionFactoryTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void init() {
        gitRevisionFactory = new GitRevisionFactory();
        gitVcs = (GitVcs) ProjectLevelVcsManager.getInstance(getProject()).findVcsByName("Git");
    }

    @Test
    public void testMatches() {
        Assert.assertFalse(gitRevisionFactory.matches(new VcsRoot(gitVcs, new LightVirtualFile()), "location", "alias"));
    }

    @Test
    public void testCreate() {
        VcsRevisionNumber revisionNumber = gitRevisionFactory.create("2a7314b614b70b1d007236cbe2bf0b0d3028693d master");
        Assert.assertTrue(revisionNumber instanceof GitRevisionNumber);
        Assert.assertEquals("2a7314b614b70b1d007236cbe2bf0b0d3028693d", ((GitRevisionNumber) revisionNumber).getRev());
    }
}
