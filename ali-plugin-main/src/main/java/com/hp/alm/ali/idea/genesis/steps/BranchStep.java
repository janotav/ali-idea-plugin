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

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.genesis.WizardContext;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;

import java.util.Arrays;

public class BranchStep extends ServerTypeAwareStep {
    public BranchStep(GenesisStep previous, WizardContext context) {
        super(previous, context, Arrays.asList(context.branch, context.branchLbl));
    }

    public void _init() {
        super._init();

        EntityList branches = EntityList.create(ctx.client.getForStream(getScmBranchTemplatePref() + "?query={0}",
                EntityQuery.encode("{release.name['" + ctx.release.getSelectedItem().toString() + "']; scm-repository.name['" + ctx.repository.getSelectedItem().toString() + "']}")));

        ctx.branch.removeAllItems();
        for (Entity entity : branches) {
            ctx.branch.addItem(new BranchWrapper(entity));
        }
    }

    public boolean isImplicitChoice() {
        return ctx.branch.getItemCount() == 1 && ctx.branch.getSelectedIndex() == 0;
    }

    public boolean isNextAvailable() {
        return ctx.branch.getSelectedIndex() >= 0;
    }
}
