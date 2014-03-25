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
import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import java.util.List;

public class ApolloAliStrategy extends ApolloStrategy {

    public ApolloAliStrategy(Project project, RestService restService) {
        super(project, restService);
    }

    @Override
    public List<DetailContent> getDetailContent(Entity entity) {
        List<DetailContent> ret = super.getDetailContent(entity);
        aliStrategyUtil.addCodeChangesDetail(this, entity, ret);
        return ret;
    }

    @Override
    public List<AliContent> getSupportedContent() {
        List<AliContent> ret = super.getSupportedContent();
        aliStrategyUtil.addBuildContent(ret);
        return ret;
    }
}
