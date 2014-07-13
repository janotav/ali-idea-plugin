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

import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public abstract class LazyComboBox extends ChooserBox<ComboItem> {

    final protected EntityService entityService;
    protected boolean loading;
    private String label;

    public LazyComboBox(Project project, String label) {
        super(project);
        this.label = label;
        this.entityService = project.getComponent(EntityService.class);
    }

    public abstract List<ComboItem> load();

    public void reload() {
        final Object selectedItem = getSelectedItem();
        removeItemListener(this);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if(getItemCount() == 0) {
                    addItem(new ComboItem("Loading..."));
                    setSelectedIndex(getItemCount() - 1);
                }
            }
        });
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                final List<ComboItem> items = load();

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        loading = true;
                        removeAllItems();
                        addItem(new ComboItem("Loading..."));
                        for(ComboItem item: items) {
                            addItem(item);
                        }
                        loading = false;
                        // we need auxiliary value to ensure that following select fires an event even if it's selecting
                        // first item (without auxiliary it would already be selected)
                        setSelectedItem(selectedItem);
                        removeItemAt(0);
                        if(getItemCount() > 1) {
                            setEnabled(true);
                        } else {
                            setEnabled(false);
                            if(getItemCount() == 0) {
                                addItem(new ComboItem("No " + label + " available"));
                            }
                        }
                        addItemListener(LazyComboBox.this);
                    }
                });
            }
        });
    }

    public void selectOrAddEntity(final Entity entity) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if(entity != null) {
                    for(int i = 0; i < getItemCount(); i++) {
                        Object item = ((ComboItem) getItemAt(i)).getKey();
                        if(entity.equals(item)) {
                            setSelectedIndex(i);
                            return;
                        }
                    }
                    addItem(new ComboItem(entity, entity.getPropertyValue("name")));
                    setSelectedIndex(getItemCount() - 1);
                } else {
                    setSelectedIndex(-1);
                }
            }
        });
    }

    public abstract class NonLoadingItemListener implements ItemListener {

        @Override
        final public void itemStateChanged(ItemEvent e) {
            if (!loading) {
                // ignore events when loading items
                doItemStateChanged(e);
            }
        }

        public abstract void doItemStateChanged(ItemEvent e);

    }
}
