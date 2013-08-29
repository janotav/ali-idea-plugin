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

package com.hp.alm.ali.idea.ui.chooser;

import com.hp.alm.ali.idea.entity.tree.EntityNode;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

class ALMTreeCellRenderer extends DefaultTreeCellRenderer {

    static private Map<String, Icon> icons;
    static {
        icons = new HashMap<String, Icon>();
        icons.put("release", IconLoader.getIcon("/release_16.png"));
        icons.put("release-cycle", IconLoader.getIcon("/release_cycle_16.png"));
        icons.put("test-set-folder.-1", IconLoader.getIcon("/actions/gc.png"));
        icons.put("favorite", IconLoader.getIcon("/favorite_16.png"));

        icons.put("requirement.type.0", IconLoader.getIcon("/types/req0.png"));
        icons.put("requirement.type.1", IconLoader.getIcon("/types/req1.png"));
        icons.put("requirement.type.2", IconLoader.getIcon("/types/req2.png"));
        icons.put("requirement.type.3", IconLoader.getIcon("/types/req3.png"));
        icons.put("requirement.type.4", IconLoader.getIcon("/types/req4.png"));
        icons.put("requirement.type.5", IconLoader.getIcon("/types/req5.png"));
        icons.put("requirement.type.6", IconLoader.getIcon("/types/req6.png"));
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int index, boolean focus) {
        EntityNode node = (EntityNode) value;
        Icon icon = getIcon(node.getEntity());
        if(icon != null) {
            setOpenIcon(icon);
            setClosedIcon(icon);
            setLeafIcon(icon);
        } else {
            setOpenIcon(getDefaultOpenIcon());
            setClosedIcon(getDefaultClosedIcon());
            setLeafIcon(getDefaultLeafIcon());
        }
        return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, index, focus);
    }

    private Icon getIcon(Entity entity) {
        String iconPath;
        if("requirement".equals(entity.getType())) {
            iconPath = entity.getType() + ".type." + entity.getPropertyValue("type-id");
        } else {
            iconPath = entity.getType() + "." + entity.getId();
        }
        Icon icon = icons.get(iconPath);
        if(icon != null) {
            return icon;
        }
        return icons.get(entity.getType());
    }
}
