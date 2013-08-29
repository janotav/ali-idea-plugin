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

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionToolbar;

public class ActionUtil {

    public static ActionToolbar createActionToolbar(String groupId, String place, boolean borderLess) {
        return createActionToolbar(groupId, place, borderLess, true);
    }

    public static ActionToolbar createActionToolbar(String groupId, String place, boolean borderLess, boolean horizontal) {
        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction(groupId);
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(place, group, horizontal);
        actionToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        if(borderLess) {
            actionToolbar.getComponent().setBorder(null);
        }
        return actionToolbar;
    }

    public static ActionPopupMenu createEntityActionPopup(String place) {
        return createActionPopup("hpali.entity", place);
    }

    public static ActionPopupMenu createActionPopup(String groupId, String place) {
        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction(groupId);
        return ActionManager.getInstance().createActionPopupMenu(place, group);
    }
}
