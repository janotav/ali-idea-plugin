/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.services.WorkspaceService;
import com.intellij.openapi.project.Project;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkspaceComboBox extends LazyComboBox {

    private WorkspaceService workspaceService;
    private Set<Integer> workspaceIds;

    public WorkspaceComboBox(final Project project, Integer workspaceId, String workspaceName, Set<Integer> workspaceIds) {
        super(project, "workspace");
        this.workspaceService = project.getComponent(WorkspaceService.class);
        this.workspaceIds = workspaceIds;

        addItem(new ComboItem(workspaceId, workspaceName != null ? workspaceName : String.valueOf(workspaceId)));

        addItemListener(new LazyComboBox.NonLoadingItemListener() {
            @Override
            public void doItemStateChanged(ItemEvent e) {
                ComboItem item = (ComboItem) e.getItem();
                if (item.getKey() instanceof Integer) {
                    workspaceService = project.getComponent(WorkspaceService.class);
                    workspaceService.selectWorkspace((Integer) item.getKey(), item.getValue());
                }
            }
        });
    }

    @Override
    public List<ComboItem> load() {
        ArrayList<ComboItem> ret = new ArrayList<ComboItem>();

        Map<Integer, String> map = workspaceService.listWorkspaces();
        for (Integer id: map.keySet()) {
            if (workspaceIds.contains(id)) {
                ret.add(new ComboItem(id, map.get(id)));
            }
        }

        return ret;
    }
}
