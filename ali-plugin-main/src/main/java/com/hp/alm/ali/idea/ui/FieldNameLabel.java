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
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.services.MetadataService;

import javax.swing.JLabel;

public class FieldNameLabel extends JLabel implements MetadataService.DispatchMetadataCallback {
    private String entityType;
    private String fieldName;
    private MetadataService metadataService;

    public FieldNameLabel(String entityType, String fieldName, MetadataService metadataService) {
        this.entityType = entityType;
        this.metadataService = metadataService;

        setFieldName(fieldName);
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
        setText("<html><i>Retrieving column name...</i></html>");
        metadataService.loadEntityMetadataAsync(entityType, this);
    }

    public String getEntityType() {
        return entityType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static String getLabel(Metadata meta, String fieldName) {
        Field field = meta.getAllFields().get(fieldName);
        if(field != null) {
            return field.getLabel();
        } else {
            return "<html><i>Unrecognized field:  "+fieldName+"</i></html>";
        }
    }

    public void metadataLoaded(Metadata metadata) {
        setText(getLabel(metadata, fieldName));
    }

    public void metadataFailed() {
        setText("<html><i>Failed to retrieve metadata</i></html>");
    }
}
