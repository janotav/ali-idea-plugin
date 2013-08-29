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

package com.hp.alm.ali.idea.action;

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.ui.FieldNameLabel;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.EntityDetails;
import com.hp.alm.ali.idea.cfg.EntityFields;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.ui.MultipleItemsDialog;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FieldsAction extends EntityAction {

    public FieldsAction() {
        super("Fields", "Select entity fields", IconLoader.getIcon("/nodes/dataColumn.png"));
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
        MetadataService metadataService = project.getComponent(MetadataService.class);
        EntityFields entityFields = conf.getFields(entity.getType());
        EntityDetails details = conf.getDetails();

        Popup popup = new Popup(project, entityFields, details, metadataService.getEntityMetadata(entity.getType()));
        popup.show(event.getInputEvent().getComponent(), 0, 0);
    }

    @Override
    protected Set<String> getSupportedPlaces() {
        return Collections.singleton("detail");
    }

    private static class Popup extends JPopupMenu {

        public Popup(final Project project, final EntityFields fields, final EntityDetails details, Metadata meta) {
            final List<Field> available = new ArrayList<Field>(meta.getAllFields().values());
            Collections.sort(available, Field.LABEL_COMPARATOR);
            final List<String> visibleFields = fields.getColumns();
            JCheckBoxMenuItem showControls = new JCheckBoxMenuItem("Show Column Controls", details.isColumnControls());
            showControls.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    details.setColumnControls(!details.isColumnControls());
                    project.getComponent(AliProjectConfiguration.class).fireColumnsChanged();

                }
            });
            add(showControls);
            add(new JSeparator());
            JMenuItem more = new JMenuItem("More Fields...");
            more.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    MultipleItemsDialog dialog = new MultipleItemsDialog(project, "Field", true, available, visibleFields);
                    dialog.setVisible(true);
                    if(dialog.isOk()) {
                        fields.setColumns(visibleFields);
                    }
                }
            });
            add(more);
            add(new JSeparator());
            for (final String column: visibleFields) {
                String columnName = FieldNameLabel.getLabel(meta, column);
                JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(columnName, true);
                if(visibleFields.size() == 1) {
                    // don't allow to remove last column
                    menuItem.setEnabled(false);
                } else {
                    menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            fields.removeColumn(column);
                        }
                    });
                }
                add(menuItem);
            }
        }
    }
}
