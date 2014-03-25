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
import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.content.detail.QueryTableLoader;
import com.hp.alm.ali.idea.content.detail.TableContent;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;

public class AliStrategyUtil {

    private Project project;

    public AliStrategyUtil(Project project) {
        this.project = project;
    }

    public void addBuildContent(List<AliContent> list) {
        list.add(BuildsContent.getInstance());
    }

    public void addCodeChangesDetail(ServerStrategy strategy, Entity entity, List<DetailContent> list) {
        if("requirement".equals(entity.getType()) ||
                "defect".equals(entity.getType()) ||
                "build-instance".equals(entity.getType())) {
            list.add(codeChangesTable(strategy, entity));
        }
    }

    private TableContent codeChangesTable(ServerStrategy strategy, Entity entity) {
        String alias = strategy.getDevelopmentAlias(entity.getType());
        EntityQuery query = new EntityQuery("changeset");
        query.getCrossFilter(entity.getType(), alias).setValue("id", String.valueOf(entity.getId()));
        return detailTable(entity, query, "Code", IconLoader.getIcon("/diff/Diff.png"), null);
    }

    public TableContent detailTable(Entity entity, EntityQuery detailQuery, String label, Icon icon, ActionToolbar toolbar) {
        return detailTable(entity, detailQuery, label, icon, toolbar, detailQuery.getPropertyMap().keySet());
    }

    private TableContent detailTable(Entity entity, EntityQuery detailQuery, String label, Icon icon, ActionToolbar toolbar, Set<String> hiddenFields) {
        return new TableContent(project, entity, label, icon, toolbar, new QueryTableLoader(project, entity, detailQuery, hiddenFields));
    }
}
