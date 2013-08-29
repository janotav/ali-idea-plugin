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

import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

class SingleTreeListener implements TreeSelectionListener {
    protected JTextField valueField;
    protected String entityType;

    public SingleTreeListener(JTextField valueField, String entityType) {
        this.valueField = valueField;
        this.entityType = entityType;
    }

    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        TreePath path = ((TreeSelectionModel)treeSelectionEvent.getSource()).getSelectionPath();
        if(path != null) {
            EntityNode node = (EntityNode)path.getLastPathComponent();
            if(node.getEntity().getType().equals(entityType)) {
                String selectedPath = pathToString(path);
                setValue(selectedPath);
            }
        }
    }

    protected void setValue(String value) {
        valueField.setText(value);
    }

    protected String pathToString(TreePath path) {
        if(path == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append("^");
            for(Object o: path.getPath()) {
                if(buf.length() > 1) {
                    buf.append("\\");
                }
                buf.append(((EntityNode)o).getEntity().getPropertyValue("name"));
            }
            buf.append("^");
            return buf.toString();
        }
    }
}
