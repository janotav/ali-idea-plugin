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

package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.cfg.EntityFields;
import com.hp.alm.ali.idea.entity.edit.DependentValue;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.ui.MultipleItemsDialog;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.ui.editor.field.EditableField;
import com.hp.alm.ali.idea.ui.editor.field.EditableFieldListener;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.ui.editor.field.LookupListField;
import com.hp.alm.ali.idea.ui.editor.field.ReferenceField;
import com.hp.alm.ali.idea.ui.editor.field.RequirementTypeField;
import com.hp.alm.ali.idea.ui.editor.field.TextField;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.model.type.UserType;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.services.ProjectUserService;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class EntityEditor extends BaseEditor implements EntityFields.ColumnsChangeListener {

    protected Project project;
    private EntityFields entityFields;
    private List<String> forceEditable;
    private List<String> columnsToEdit;
    private Metadata metadata;
    private Context context;

    public EntityEditor(final Project project, String titleTemplate, final Entity entity, List<String> columnsToEdit, boolean addRequired, boolean removeReadonly, List<String> forceEditable, SaveHandler saveHandler) {
        super(project, "", entity, saveHandler);

        this.project = project;
        this.columnsToEdit = columnsToEdit;
        this.forceEditable = forceEditable;

        setEditorTitle(project, titleTemplate, entity.getType());

        metadata = project.getComponent(MetadataService.class).getEntityMetadata(entity.getType());
        entityFields = project.getComponent(AliProjectConfiguration.class).getFields(entity.getType());
        entityFields.addColumnsChangeListener(this);

        if(addRequired) {
            for(Field field: metadata.getRequiredFields()) {
                if(!columnsToEdit.contains(field.getName()) && field.getRelatedType() == null) {
                    columnsToEdit.add(field.getName());
                }
            }
        }
        for(Iterator<String> it = columnsToEdit.iterator(); it.hasNext(); ) {
            Field field = metadata.getAllFields().get(it.next());
            if(field == null || (removeReadonly && !field.isEditable() && !forceEditable.contains(field.getName()))) {
                it.remove();
            }
        }

        JTextPane addProperty = new JTextPane();
        addProperty.setEditorKit(new HTMLEditorKit());
        addProperty.setText("<html><body><i>Property not listed? Click <a href='#addProperty'>here</a></i></body></html>");
        addProperty.setEditable(false);
        addProperty.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    handleAddProperty();
                }
            }
        });
        gridFooter.setLayout(new BorderLayout());
        gridFooter.add(addProperty, BorderLayout.EAST);
    }

    @Override
    public void update() {
        context = new Context(entity, this);

        for(final String column: columnsToEdit) {
            Field field = metadata.getAllFields().get(column);
            boolean editable = field.isEditable() || forceEditable.contains(column);

            addField(field, editable);
        }

        packAndPosition();
    }

    private void handleAddProperty() {
        List<Field> available = new ArrayList<Field>();
        for (Field field : metadata.getAllFields().values()) {
            // offer all editable fields that are currently not present
            if (fields.get(field.getName()) == null && field.isEditable()) {
                available.add(field);
            }
        }
        Collections.sort(available, Field.LABEL_COMPARATOR);
        LinkedList<String> addedFields = new LinkedList<String>();
        MultipleItemsDialog dialog = new MultipleItemsDialog(project, "Field", true, available, addedFields);
        dialog.setVisible(true);
        if (dialog.isOk()) {
            entityFields.addColumns(addedFields);
        }
    }

    @Override
    public void columnsChanged(String columnToFocus) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                boolean changed = false;
                for (String field : entityFields.getColumns()) {
                    if (getField(field) != null) {
                        continue;
                    }
                    Field f = metadata.getField(field);
                    if (f == null) {
                        // TODO: enforce field validity instead (store fields per server type)
                        continue;
                    }
                    addField(f, f.isEditable());
                    changed = true;
                }
                if (changed) {
                    gridPanel.revalidate();
                    gridPanel.repaint();
                }
            }
        });
    }

    private void addField(final Field field, boolean editable) {
        String value = entity.getPropertyValue(field.getName());
        if(field.isBlob()) {
            if(field.getName().equals("dev-comments") || field.getName().equals("comments")) {
                String userName = project.getComponent(AliProjectConfiguration.class).getUsername();
                String fullName = project.getComponent(ProjectUserService.class).getUserFullName(userName);

                addField(field.getName(), new CommentField(field.getLabel(), value, userName, fullName), true);
            } else {
                addField(field.getName(), new HTMLAreaField(field.getLabel(), value, field.isRequired(), editable), true);
            }
        } else if(field.getClazz().equals(UserType.class)) {
            addField(field.getName(), new ReferenceField(project, field, context, editable));
        } else if(field.getListId() != null) {
            List<String> list = project.getComponent(ProjectListService.class).getProjectList(entity.getType(), field);
            addField(field.getName(), new LookupListField(list, field, entity, editable));
        } else if("requirement".equals(entity.getType()) && "type-id".equals(field.getName())) {
            addField(field.getName(), new RequirementTypeField(project, field, entity, editable));
        } else if(field.isReference()) {
            addField(field.getName(), new ReferenceField(project, field, context, editable));
        } else {
            addField(field.getName(), new TextField(field.getLabel(), value, field.isRequired(), editable));
        }
        if(DependentValue.class.isAssignableFrom(field.getClazz())) {
            getField(field.getName()).addUpdateListener(new EditableFieldListener() {
                @Override
                public void updated(EditableField editableField) {
                    ((DependentValue)project.getComponent(field.getClazz())).valueChanged(EntityEditor.this, editableField.getValue());
                }
            });
        }
    }

    public void setFieldValue(String name, String value) {
        setFieldValue(name, value, true);
    }

    public void setFieldValue(String name, String value, boolean force) {
        EditableField uiField = getField(name);
        if(uiField != null) {
            uiField.setValue(value);
        } else if(force) {
            Field metaField = metadata.getAllFields().get(name);
            if(metaField != null) {
                addField(metaField, metaField.isEditable());
                getField(name).setValue(value);
                ((JComponent)getContentPane()).revalidate();
            }
        }
    }

    public void refresh(Entity entity, boolean refreshDirty) {
        for (String name: fields.keySet()) {
            if(!entity.isInitialized(name)) {
                continue;
            }
            EditableField editableField = fields.get(name);
            String currentValue = entity.getPropertyValue(name);
            if(refreshDirty || !editableField.hasChanged()) {
                editableField.setOriginalValue(currentValue);
                editableField.setValue(currentValue);
            } else if(currentValue.equals(editableField.getValue())) {
                editableField.setOriginalValue(currentValue);
            }
        }
    }

    public static class Create extends BaseHandler {

        public Create(Project project) {
            super(project);
        }

        protected void entityCreated(Entity entity) {
        }

        @Override
        public boolean save(Entity modified, Entity base) {
            Entity entity = entityService.createEntity(modified, false);
            if(entity != null) {
                entityCreated(entity);
                return true;
            } else {
                return false;
            }
        }
    }
}
