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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.ActiveItemService;
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

public class ActiveItemLink extends LinkLabel implements ActiveItemService.Listener, LinkListener {

    private EntityLabelService entityLabelService;
    private ActiveItemService activeItemService;
    private EntityRef ref;

    public ActiveItemLink(Project project) {
        super("No active item", ActiveItemService.activeItemIcon);

        setListener(this, null);

        entityLabelService = project.getComponent(EntityLabelService.class);
        activeItemService = project.getComponent(ActiveItemService.class);
        activeItemService.addListener(this);
        doActivate(activeItemService.getActiveItem());
    }

    public void onActivated(EntityRef item) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        doActivate(item);
    }

    private void doActivate(final EntityRef ref) {
        this.ref = ref;

        if(ref != null) {
            entityLabelService.loadEntityLabelAsync(ref.type, new AbstractCachingService.DispatchCallback<String>() {
                @Override
                public void loaded(String entityLabel) {
                    setText(entityLabel + " #"+ref.id);
                }
            });
            setEnabled(true);
        } else {
            setText("No active item");
            setEnabled(false);
        }
    }

    public void linkSelected(LinkLabel aSource, Object aLinkData) {
        if(ref != null) { // the link remains clickable even when disabled
            activeItemService.selectEntityDetail(new Entity(ref.type, ref.id));
        }
    }
}
