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

import com.hp.alm.ali.idea.cfg.AliConfigurable;
import com.hp.alm.ali.idea.genesis.WizardContext;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.ui.Messages;

import java.util.Arrays;

public class ReleaseStep extends GenesisStep {
    public ReleaseStep(GenesisStep previous, WizardContext ctx) {
        super(previous, ctx, Arrays.asList(ctx.release, ctx.releaseLbl));
    }

    public void _init() {
        super._init();

        ctx.client.setProject((String) ctx.project.getSelectedItem());
        ctx.release.removeAllItems();

        ctx.serverType = AliConfigurable.getServerType(ctx.location.getText(), (String) ctx.domain.getSelectedItem(),
                (String) ctx.project.getSelectedItem(), ctx.username.getText(), ctx.password.getText());

        switch (ctx.serverType) {
            case ALI:
            case ALI2:
            case ALI11_5:
            case ALI12:
            case AGM:
                EntityList releases = EntityList.create(ctx.client.getForStream("releases"));
                for (Entity entity : releases) {
                    ctx.release.addItem(entity.getProperty("name"));
                }
                break;

            default:
                Messages.showInfoMessage("ALI Extension is not enabled in the specified project, environment provisioning is not supported.", "Not Available");
        }
    }

    public boolean isImplicitChoice() {
        return ctx.release.getItemCount() == 1 && ctx.release.getSelectedIndex() == 0;
    }

    public boolean isNextAvailable() {
        return ctx.release.getSelectedIndex() >= 0;
    }
}
