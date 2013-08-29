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
import com.hp.alm.ali.idea.services.EntityLabelService;
import com.intellij.openapi.project.Project;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ChooseEntityTypePopup extends JPopupMenu {

    public ChooseEntityTypePopup(final Project project, List<String> types, final Listener listener) {
        final EntityLabelService entityLabelService = project.getComponent(EntityLabelService.class);

        for(final String target: types) {
            final JMenuItem menuItem = new JMenuItem(target);
            entityLabelService.loadEntityLabelAsync(target, new AbstractCachingService.DispatchCallback<String>() {
                @Override
                public void loaded(String entityLabel) {
                    menuItem.setText(entityLabel);
                }
            });
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.selected(target);
                }
            });
            add(menuItem);
        }
    }

    public void showOrInvokeDirectly(Component comp, int x, int y) {
        if(getComponentCount() == 1) {
            ((JMenuItem)getComponent(0)).getActionListeners()[0].actionPerformed(null);
        } else {
            super.show(comp, x, y);
        }
    }

    public interface Listener {

        void selected(String entityType);

    }
}
