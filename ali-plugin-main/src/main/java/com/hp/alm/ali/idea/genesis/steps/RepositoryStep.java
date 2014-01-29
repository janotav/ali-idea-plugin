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

public class RepositoryStep extends ServerTypeAwareStep {
    public RepositoryStep(GenesisStep previous, WizardContext ctx) {
        super(previous, ctx, Arrays.asList(ctx.repository, ctx.repositoryLbl));
    }

    public void _init() {
        super._init();

        // FIXME: support release-repository relation directly
        EntityList branches = EntityList.create(ctx.client.getForStream(getScmBranchTemplatePref() + "?query={0}",
                EntityQuery.encode("{release.name['" + ctx.release.getSelectedItem().toString() + "']}")));

        EntityList repos = EntityList.create(ctx.client.getForStream(getScmRepositoriesTemplatePref()));
        ctx.repository.removeAllItems();
        for(Entity entity: repos) {
            for(Entity b: branches) {
                if(b.getProperty("parent-id").equals(entity.getProperty("id"))) {
                    ctx.repository.addItem(new EntityWrapper(entity));
                    break;
                }
            }
        }
    }

    public boolean isImplicitChoice() {
        return ctx.repository.getItemCount() == 1 && ctx.repository.getSelectedIndex() == 0;
    }

    public boolean isNextAvailable() {
        return ctx.repository.getSelectedIndex() >= 0;
    }
}
