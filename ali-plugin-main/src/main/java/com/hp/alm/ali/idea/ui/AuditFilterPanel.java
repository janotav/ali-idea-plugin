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

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.Audit;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuditFilterPanel extends JPanel {
    private List<String> selectedFields;
    private List<String> selectedUsers;
    private Project project;
    private Listener listener;

    public AuditFilterPanel(Project project, List<Audit> audits, Listener listener) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.project = project;
        this.listener = listener;

        Map<String, Field> propertyFields = new HashMap<String, Field>();
        Map<String, Field> userFields = new HashMap<String, Field>();
        for(Audit audit: audits) {
            for(String[] prop: audit.getProperties()) {
                propertyFields.put(prop[0], new Field(prop[0], prop[0]));
            }
            userFields.put(audit.getUsername(), new Field(audit.getUsername(), audit.getUsername()));
        }

        selectedFields = new ArrayList<String>();
        selectedUsers = new ArrayList<String>();

        addFilter("Fields:", "Field", propertyFields, selectedFields);
        addFilter("Users:", "User", userFields, selectedUsers);
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public List<String> getSelectedUsers() {
        return selectedUsers;
    }

    private void addFilter(String labelTitle, String dialogTitle, Map<String, Field> fields, List<String> selectedFields) {
        add(new JLabel(labelTitle));
        LinkLabel link = new LinkLabel(asText(selectedFields), null);
        link.setListener(new MyLinkListener(dialogTitle, fields, selectedFields), link);
        add(link);
    }

    private String asText(List<String> selected) {
        if(selected.isEmpty()) {
            return "<all>";
        } else {
            if(selected.size() > 3) {
                return StringUtil.join(selected.subList(0, 2).toArray(new String[0]), ";") + " and " + (selected.size() - 2) + " more";
            } else {
                return StringUtil.join(selected.toArray(new String[0]), ";");
            }
        }
    }

    private class MyLinkListener implements LinkListener {
        private String title;
        private Map<String, Field> fields;
        private List<String> selected;

        private MyLinkListener(String title, Map<String, Field> fields, List<String> selected) {
            this.title = title;
            this.fields = fields;
            this.selected = selected;
        }

        public void linkSelected(LinkLabel aSource, Object aLinkData) {
            ArrayList<String> visible = new ArrayList<String>(selected);
            MultipleItemsDialog dialog = new MultipleItemsDialog(project, title, true, new ArrayList<Field>(fields.values()), visible);
            dialog.setVisible(true);
            if(dialog.isOk()) {
                if(!new HashSet<String>(visible).equals(new HashSet<String>(selected))) {
                    selected.clear();
                    selected.addAll(visible);
                    ((LinkLabel)aLinkData).setText(asText(selected));
                    listener.changed(new HashSet<String>(getSelectedFields()), new HashSet<String>(getSelectedUsers()));
                }
            }
        }
    }

    public static interface Listener {

        void changed(Set<String> selectedFields, Set<String> selectedUsers);

    }
}
