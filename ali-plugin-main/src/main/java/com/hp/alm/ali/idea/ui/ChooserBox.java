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

import com.intellij.openapi.project.Project;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ChooserBox<E> extends JComboBox implements ItemListener {

    private int lastSelectedIndex;
    final protected Project project;

    public ChooserBox(Project project) {
        this.project = project;
        addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED) {
            if(e.getItem() instanceof ChoosingItem) {
                hidePopup();
                showChooser((ChoosingItem<E>)e.getItem());
            }
            lastSelectedIndex = getSelectedIndex();
        }
    }

    private void showChooser(ChoosingItem<E> chooser) {
        TableFilterDialog<E> filterDialog = new TableFilterDialog<E>(project, chooser.toString(), false, "Name",
                chooser.getItemsProvider(), chooser.getItemRenderer());
        filterDialog.setVisible(true);
        if(filterDialog.isOk()) {
            E selected = filterDialog.getSelectedItem();
            Object comboItem = chooser.create(selected);
            for(int i = 0; i < getItemCount(); i++) {
                if(getItemAt(i).equals(comboItem)) {
                    setSelectedIndex(i);
                    return;
                }
            }
            addItem(comboItem);
            setSelectedIndex(getItemCount() - 1);
        } else {
            setSelectedIndex(lastSelectedIndex);
        }
    }
}
