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
import com.hp.alm.ali.idea.navigation.Recognizer;
import com.hp.alm.ali.idea.util.BrowserUtil;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebLinkRecognizer implements Recognizer {

    private static final Pattern WEB_PATTERN = Pattern.compile("https?://[^ ]+");

    @Override
    public void recognize(String content, List<Candidate> candidates) {
        Matcher matcher = WEB_PATTERN.matcher(content);
        while(matcher.find()) {
            candidates.add(new Candidate(matcher.start(), matcher.end(), "web:" + matcher.group()));
        }
    }

    @Override
    public boolean navigate(Project project, String hyperlink) {
        if(hyperlink.startsWith("web:")) {
            project.getComponent(BrowserUtil.class).launchBrowser(EntityQuery.decode(hyperlink.substring(4)));
            return true;
        } else {
            return false;
        }
    }
}
