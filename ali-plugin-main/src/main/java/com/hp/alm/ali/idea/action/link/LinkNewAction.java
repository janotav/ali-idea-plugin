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

package com.hp.alm.ali.idea.action.link;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.ui.editor.EntityEditor;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.ui.ChooseEntityTypePopup;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LinkNewAction extends EntityAction {

    private static Set<String> entityTypes;
    static {
        entityTypes = new HashSet<String>();
        entityTypes.add("defect");
        entityTypes.add("requirement");
    }

    public LinkNewAction() {
        super("Link", "Link entity", IconLoader.getIcon("/general/add.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return entityTypes;
    }

    @Override
    protected void actionPerformed(AnActionEvent event, final Project project, final Entity entity) {
        LinkedList<String> types = new LinkedList<String>();
        types.add("defect");
        if("defect".equals(entity.getType())) {
            types.add("requirement");
        }
        ChooseEntityTypePopup popup = new ChooseEntityTypePopup(project, types, new ChooseEntityTypePopup.Listener() {
            @Override
            public void selected(String targetType) {
                Entity link = new Entity("defect-link", 0);

                String endpoint;
                String otherEndpoint;
                if("defect".equals(entity.getType())) {
                    link.setProperty("first-endpoint-id", String.valueOf(entity.getId()));
                    link.setProperty("second-endpoint-type", targetType);
                    endpoint = "second-endpoint-id";
                    otherEndpoint = "first-endpoint-id";
                } else {
                    link.setProperty("second-endpoint-id", String.valueOf(entity.getId()));
                    link.setProperty("second-endpoint-type", entity.getType());
                    endpoint = "first-endpoint-id";
                    otherEndpoint = "second-endpoint-id";
                }

                List<String> columns;
                boolean linkTypeEditable = project.getComponent(RestService.class).getServerStrategy().getDefectLinkColumns().contains("link-type");
                if(linkTypeEditable) {
                    columns = new ArrayList<String>(Arrays.asList(endpoint, "comment", "link-type", otherEndpoint, "second-endpoint-type"));
                } else {
                    columns = new ArrayList<String>(Arrays.asList(endpoint, "comment", otherEndpoint, "second-endpoint-type"));
                }
                EntityEditor entityEditor = new EntityEditor(project, "Create link to {0}", link, columns, true, false, Arrays.asList(endpoint), new EntityEditor.Create(project));
                entityEditor.execute();
            }
        });
        popup.showOrInvokeDirectly(event.getInputEvent().getComponent(), 0, 0);
    }
}
