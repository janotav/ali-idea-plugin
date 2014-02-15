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

import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.content.detail.TableContent;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.List;

public class AliStrategy extends MayaStrategy {

    public AliStrategy(Project project, RestService restService) {
        super(project, restService);
    }

    @Override
    public List<Relation> getRelationList(String entityType) {
        List<Relation> list = super.getRelationList(entityType);

        if("defect".equals(entityType)) {
            list.addAll(relationList("changeset"));
        }

        return list;
    }

    @Override
    public List<DetailContent> getDetailContent(Entity entity) {
        List<DetailContent> ret = super.getDetailContent(entity);
        if("requirement".equals(entity.getType()) ||
                "defect".equals(entity.getType()) ||
                "build-instance".equals(entity.getType())) {
            ret.add(codeChangesTable(entity));
        }
        return ret;
    }

    private TableContent codeChangesTable(Entity entity) {
        String alias = getDevelopmentAlias(entity.getType());
        EntityQuery query = new EntityQuery("changeset");
        query.getCrossFilter(entity.getType(), alias).setValue("id", String.valueOf(entity.getId()));
        return detailTable(entity, query, "Code", IconLoader.getIcon("/diff/Diff.png"), null);
    }
}
