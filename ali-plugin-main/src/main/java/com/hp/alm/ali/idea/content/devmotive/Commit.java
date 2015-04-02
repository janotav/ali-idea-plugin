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

package com.hp.alm.ali.idea.content.devmotive;

import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;

import java.util.Date;

final public class Commit {
    final private VcsFileRevision revision;
    final private String authorName;
    final private String authorEmail;
    final private String committerName;
    final private String committerEmail;

    public Commit(VcsFileRevision revision, String authorName,
                  String authorEmail, String committerName, String committerEmail) {
        this.revision = revision;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
    }

    public VcsFileRevision getRevision() {
        return revision;
    }

    public VcsRevisionNumber getRevisionNumber() {
        return revision.getRevisionNumber();
    }

    public String getMessage() {
        return revision.getCommitMessage();
    }

    public Date getDate() {
        return revision.getRevisionDate();
    }

    public String getRevisionString() {
        return revision.getRevisionNumber().asString();
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getCommitterName() {
        return committerName;
    }

    public String getCommitterEmail() {
        return committerEmail;
    }
}
