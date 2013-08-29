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

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.HashSet;
import java.util.Set;

public class ViewDetailAction extends EntityAction {

    private static Set<String> entityTypes;
    static {
        entityTypes = new HashSet<String>();
        entityTypes.add("defect");
        entityTypes.add("requirement");
        entityTypes.add("build-instance");
        entityTypes.add("release-backlog-item"); // make sure we are visible in the EntityQuery("release-backlog-item") context
    }

    public ViewDetailAction() {
        super("Detail", "View entity detail", IconLoader.getIcon("/actions/preview.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return entityTypes;
    }

    @Override
    protected void update(AnActionEvent event, Project project, Entity entity) {
        if("detail".equals(event.getPlace())) {
            event.getPresentation().setEnabled(false);
            event.getPresentation().setVisible(false);
        }
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        AliContentFactory.loadDetail(project, entity, true, true);
    }
}
