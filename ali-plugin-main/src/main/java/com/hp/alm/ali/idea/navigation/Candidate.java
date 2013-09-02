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

package com.hp.alm.ali.idea.navigation;

import com.intellij.openapi.project.Project;

public class Candidate implements Comparable<Candidate> {

    private int start;
    private int end;

    private int linkStart;
    private int linkEnd;

    private String hyperLink;

    public Candidate(int start, int end, String hyperLink) {
        this(start, end, start, end);

        this.hyperLink = hyperLink;
    }

    protected Candidate(int start, int end, int linkStart, int linkEnd) {
        if(start < 0) {
            throw new IllegalArgumentException("start < 0");
        }
        if(end <= start) {
            throw new IllegalArgumentException("end <= start");
        }
        if(linkStart < start) {
            throw new IllegalArgumentException("linkStart < start");
        }
        if(linkEnd > end) {
            throw new IllegalArgumentException("linkEnd > end");
        }
        if(linkEnd <= linkStart) {
            throw new IllegalArgumentException("linkEnd <= linkStart");
        }

        this.start = start;
        this.end = end;
        this.linkStart = linkStart;
        this.linkEnd = linkEnd;
    }

    public String createLink(Project project) {
        return hyperLink;
    }

    public int getStart() {
        return start;
    }

    @Override
    public int compareTo(Candidate o) {
        return start - o.start;
    }

    public int getEnd() {
        return end;
    }

    public int getLinkStart() {
        return linkStart;
    }

    public int getLinkEnd() {
        return linkEnd;
    }

    /**
     * @return unresolved hyperlink value
     * @see #createLink(com.intellij.openapi.project.Project)
     */
    public String getHyperLink() {
        return hyperLink;
    }
}
