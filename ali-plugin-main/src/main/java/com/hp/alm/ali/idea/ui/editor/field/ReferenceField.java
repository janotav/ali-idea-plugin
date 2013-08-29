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

package com.hp.alm.ali.idea.ui.editor.field;

import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.filter.FilterFactoryImpl;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.action.UndoAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ReferenceField extends BaseField {

    private TextFieldWithBrowseButton textFieldWithBrowseButton;
    private TranslateService translateService;
    private String selectedId;
    private boolean isCompound;
    private Field field;

    public ReferenceField(final Project project, final Field field, Context context, boolean editable) {
        super(field.getLabel(), field.isRequired(), context.getEntity().getPropertyValue(field.getName()));

        this.field = field;

        translateService = project.getComponent(TranslateService.class);

        Entity entity = context.getEntity();
        selectedId = getOriginalValue();

        textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        textFieldWithBrowseButton.getTextField().setEditable(editable);
        textFieldWithBrowseButton.setEnabled(editable);

        String value = entity.getPropertyValue(field.getName());
        isCompound = translateService.isTranslated(field);
        if(isCompound && !value.isEmpty()) {
            translateService.translateAsync(field, value, true, new ValueCallback() {
                @Override
                public void value(String value) {
                    textFieldWithBrowseButton.getTextField().setText(value);
                }
            });
        } else {
            textFieldWithBrowseButton.getTextField().setText(value);
        }

        if(editable) {
            FilterFactory filterFactory = project.getComponent(FilterManager.class).getFilterFactory(context, entity.getType(), field, false);
            if(filterFactory == null) {
                String target = field.resolveReference(entity);
                filterFactory = new FilterFactoryImpl(project, target, false, true);
            }

            textFieldWithBrowseButton.setEditable(!isCompound);
            if(textFieldWithBrowseButton.isEditable()) {
                UndoAction.installUndoRedoSupport(textFieldWithBrowseButton.getTextField());
            }
            final FilterFactory factory = filterFactory;
            textFieldWithBrowseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    FilterChooser chooser = factory.createChooser(selectedId);
                    chooser.show();
                    selectedId = chooser.getSelectedValue();
                    selectIdUpdated();
                }
            });
            textFieldWithBrowseButton.getTextField().getDocument().addDocumentListener(new MyDocumentListener(this) {
                protected void updated() {
                    if (!isCompound) {
                        selectedId = textFieldWithBrowseButton.getText();
                    }
                    super.updated();
                }
            });
        }
    }

    private void selectIdUpdated() {
        if (!selectedId.isEmpty()) {
            if(isCompound) {
                translateService.translateAsync(field, selectedId, true, new ValueCallback() {
                    @Override
                    public void value(String value) {
                        textFieldWithBrowseButton.getTextField().setText(value);
                    }
                });
            } else {
                textFieldWithBrowseButton.setText(selectedId);
            }
        } else {
            textFieldWithBrowseButton.setText("");
        }
    }

    public Component getComponent() {
        return textFieldWithBrowseButton;
    }

    public String getValue() {
        return selectedId;
    }

    @Override
    public void setValue(String value) {
        this.selectedId = value;
        fireUpdated();
        selectIdUpdated();
    }
}
