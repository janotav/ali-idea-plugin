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

package com.hp.alm.ali.idea.checkin;

import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;

public class AliCheckinHandlerFactory extends CheckinHandlerFactory {

    public CheckinHandler createHandler(CheckinProjectPanel checkinProjectPanel, CommitContext commitContext) {
        // attempt to initialize connection if not yet initialized
        // work item chooser in the commit dialog requires working connection
        // and user may attempt to commit without ever opening the HP ALI tool window
        checkinProjectPanel.getProject().getComponent(RestService.class).checkConnectivity();
        return new AliCheckinHandler(checkinProjectPanel);
    }
}
