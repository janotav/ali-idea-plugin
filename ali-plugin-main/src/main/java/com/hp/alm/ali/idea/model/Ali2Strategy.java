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

package com.hp.alm.ali.idea.model;

import com.hp.alm.ali.idea.content.AliContent;
import com.hp.alm.ali.idea.content.builds.BuildsContent;
import com.hp.alm.ali.idea.content.defect.DefectsContent;
import com.hp.alm.ali.idea.content.requirements.RequirementsContent;
import com.hp.alm.ali.idea.content.settings.SettingsContent;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import java.util.Arrays;
import java.util.List;

public class Ali2Strategy extends AliStrategy {

    public Ali2Strategy(Project project, RestService restService) {
        super(project, restService);
    }

    @Override
    public List<Relation> getRelationList(String entityType) {
        List<Relation> list = super.getRelationList(entityType);

        if("defect".equals(entityType)) {
            list.addAll(relationList("build-instance")); // with activity in build
        }

        if("requirement".equals(entityType)) {
            list.addAll(relationList("build-instance")); // with activity in build
        }

        if("build-instance".equals(entityType)) {
            list.addAll(relationList(
                    "changeset", // containing commits
                    "defect", //  with development activity on
                    "requirement")); // with development activity on
        }

        return list;
    }

    @Override
    public List<AliContent> getSupportedContent() {
        return Arrays.asList(
                DefectsContent.getInstance(),
                RequirementsContent.getInstance(),
                BuildsContent.getInstance(),
                SettingsContent.getInstance());
    }
}
