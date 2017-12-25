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

import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.idea.svn.SvnRevisionNumber;
import org.jetbrains.idea.svn.SvnVcs;
import org.jetbrains.idea.svn.api.Revision;

public class SubversionRevisionFactory implements RevisionFactory {
    @Override
    public VcsRevisionNumber create(String revision) {
        try {
            return new SvnRevisionNumber(Revision.of(Long.valueOf(revision)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean matches(VcsRoot vcsRoot, String location, String alias) {
        SvnVcs svnVcs = (SvnVcs) vcsRoot.getVcs();
        String url = svnVcs.getInfo(vcsRoot.getPath()).getRepositoryRootURL().toString();
        return url.equals(location) || url.equals(alias);
    }

    @Override
    public String getType() {
        return "svn";
    }
}
