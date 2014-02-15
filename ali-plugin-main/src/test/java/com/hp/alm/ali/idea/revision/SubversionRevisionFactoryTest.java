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
import org.jetbrains.idea.svn.SvnRevisionNumber;
import org.jetbrains.idea.svn.SvnVcs;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.SVNURL;

public class SubversionRevisionFactoryTest extends IntellijTest {

    private SubversionRevisionFactory subversionRevisionFactory;
    private SvnVcs svnVcs;

    public SubversionRevisionFactoryTest() {
        super(ServerVersion.AGM);
    }

    @Before
    public void init() {
        subversionRevisionFactory = new SubversionRevisionFactory();
        svnVcs = (SvnVcs)ProjectLevelVcsManager.getInstance(getProject()).findVcsByName("svn");
    }

    @Test
    public void testMatches() throws SVNException {
        SVNInfo info = Mockito.mock(SVNInfo.class);
        Mockito.when(info.getRepositoryRootURL()).thenReturn(SVNURL.parseURIDecoded("http://host/svn/repo"));
        SvnVcs spy = Mockito.spy(svnVcs);
        LightVirtualFile file = new LightVirtualFile();
        Mockito.doReturn(info).when(spy).getInfo(file);

        Assert.assertFalse(subversionRevisionFactory.matches(new VcsRoot(spy, file), "location", "alias"));
        Assert.assertTrue(subversionRevisionFactory.matches(new VcsRoot(spy, file), "http://host/svn/repo", "alias"));
        Assert.assertTrue(subversionRevisionFactory.matches(new VcsRoot(spy, file), "location", "http://host/svn/repo"));
    }

    @Test
    public void testCreate() {
        VcsRevisionNumber revisionNumber = subversionRevisionFactory.create("1234");
        Assert.assertTrue(revisionNumber instanceof SvnRevisionNumber);
        Assert.assertEquals(1234, ((SvnRevisionNumber) revisionNumber).getLongRevisionNumber());
    }
}
