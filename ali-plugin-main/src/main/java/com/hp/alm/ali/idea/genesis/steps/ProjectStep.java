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

package com.hp.alm.ali.idea.genesis.steps;

import com.hp.alm.ali.idea.genesis.WizardContext;

import java.util.Arrays;
import java.util.List;

public class ProjectStep extends GenesisStep {

    private String defaultProject;

    public ProjectStep(GenesisStep previous, WizardContext ctx, String defaultProject) {
        super(previous, ctx, Arrays.asList(ctx.project, ctx.projectLbl));
        this.defaultProject = defaultProject;
    }

    public void _init() {
        super._init();

        ctx.client.setDomain((String) ctx.domain.getSelectedItem());
        ctx.client.setProject(null);
        List<String> projects = ctx.client.listCurrentProjects();

        int idx = ctx.project.getSelectedIndex();
        if(idx < 0) {
            idx = projects.indexOf(defaultProject);
        }
        ctx.project.removeAllItems();
        for(String s: projects) {
            ctx.project.addItem(s);
        }
        if(idx >= 0) {
            ctx.project.setSelectedIndex(idx);
        }
    }

    public boolean isImplicitChoice() {
        return ctx.project.getItemCount() == 1 && ctx.project.getSelectedIndex() == 0;
    }

    public boolean isNextAvailable() {
        return ctx.project.getSelectedIndex() >= 0;
    }
}
