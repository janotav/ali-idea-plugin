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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.tasks.TaskManagerIntegration;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;
import org.jdom.Element;

import javax.swing.Icon;

@State(
  name = "ActiveItemService",
  storages = { @Storage(id = "default",file = "$WORKSPACE_FILE$") }
)
public class ActiveItemService implements PersistentStateComponent<Element>, TaskManagerIntegration.Listener {
    public static Icon activeItemIcon =  IconLoader.getIcon("/general/secondaryGroup.png");

    private WeakListeners<Listener> listeners = new WeakListeners<Listener>();
    private EntityRef ref;
    private Project project;
    private TaskManagerIntegration taskManagerIntegration;

    public ActiveItemService(Project project) {
        this.project = project;
        this.taskManagerIntegration = project.getComponent(TaskManagerIntegration.class);

        if(taskManagerIntegration != null) {
            taskManagerIntegration.addListener(this);
        }
    }

    public synchronized void activate(Entity entity, boolean callTaskManager, boolean select) {
        if(entity != null) {
            ref = new EntityRef(entity);
        } else {
            ref = null;
        }
        fireOnActivated();

        if(select && entity != null) {
            selectEntityDetail(entity);
        }

        if(callTaskManager && taskManagerIntegration != null) {
            taskManagerIntegration.activate(entity);
        }
    }

    public synchronized EntityRef getActiveItem() {
        return ref;
    }

    public void selectEntityDetail(final Entity entity) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                AliContentFactory.loadDetail(project, entity, true, true);
            }
        });
    }

    private void fireOnActivated() {
        listeners.fire(new WeakListeners.Action<Listener>() {
            public void fire(Listener listener) {
                listener.onActivated(ref);
            }
        });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Element getState() {
        Element element = new Element(getClass().getSimpleName());
        if(ref != null) {
            element.setAttribute("type", ref.type);
            element.setAttribute("id", String.valueOf(ref.id));
        }
        return element;
    }

    public void loadState(Element state) {
        String type = state.getAttributeValue("type");
        if(type != null) {
            ref = new EntityRef(type, Integer.valueOf(state.getAttributeValue("id")));
        }
    }

    public void taskEntityActivated(Entity entity) {
        activate(entity, false, true);
    }

    public static interface Listener {

        void onActivated(EntityRef ref);

    }

}
