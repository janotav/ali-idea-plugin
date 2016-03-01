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
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.rest.client.RestClient;
import com.intellij.ide.wizard.CommitStepException;

import java.util.Arrays;

public class ALMStep extends GenesisStep {

    public ALMStep(WizardContext ctx) {
        super(null, ctx, Arrays.asList(ctx.location, ctx.locationLbl, ctx.usernameLbl,
                ctx.username, ctx.passwordLbl, ctx.password));
    }
    public void _commit(boolean finishChosen) throws CommitStepException {
        ctx.client = RestService.createRestClient(ctx.location.getText(), null, null, ctx.username.getText(), ctx.password.getText(), RestClient.SessionStrategy.AUTO_LOGIN);
        try {
            ctx.client.login();
        } catch (Exception e) {
            throw new CommitStepException("Could not login into HPE ALM, verify location parameter and credentials");
        }
    }

    public boolean isNextAvailable() {
        return !ctx.location.getText().isEmpty() && !ctx.username.getText().isEmpty();
    }
}
