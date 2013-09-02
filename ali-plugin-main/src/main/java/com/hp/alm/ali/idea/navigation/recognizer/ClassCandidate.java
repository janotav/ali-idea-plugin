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

package com.hp.alm.ali.idea.navigation.recognizer;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.navigation.Candidate;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;

public class ClassCandidate extends Candidate {

    private String className;
    private String fileName;
    private int line;
    private String methodName;

    public ClassCandidate(int start, int end, int linkStart, int linkEnd, String className, String fileName, int line, String methodName) {
        super(start, end, linkStart, linkEnd);

        this.className = className;
        this.fileName = fileName;
        this.line = line;
        this.methodName = methodName;
    }

    @Override
    public String createLink(Project project) {
        for(ChooseByNameContributor contributor: ChooseByNameRegistry.getInstance().getClassModelContributors()) {
            NavigationItem[] byName = contributor.getItemsByName(fileName, fileName, project, true);
            if(byName.length > 0) {
                StringBuffer buf = new StringBuffer();
                buf.append("goto:c=");
                buf.append(EntityQuery.encode(className));
                buf.append("&f=");
                buf.append(EntityQuery.encode(fileName));
                if(line > 0) {
                    buf.append("&l=");
                    buf.append(line);
                }
                if(methodName != null) {
                    buf.append("&m=");
                    buf.append(EntityQuery.encode(methodName));
                }
                return buf.toString();
            }
        }
        return null;
    }

    public String getClassName() {
        return className;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLine() {
        return line;
    }
}
