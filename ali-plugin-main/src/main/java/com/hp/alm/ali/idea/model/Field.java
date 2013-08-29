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

package com.hp.alm.ali.idea.model;

import java.util.Comparator;

public class Field implements KeyValue {

    public static Comparator<Field> LABEL_COMPARATOR = new Comparator<Field>() {
        public int compare(Field field1, Field field2) {
            return field1.getLabel().compareTo(field2.getLabel());
        }
    };

    private String name;
    private String label;
    private Class clazz = String.class;
    private Integer listId;
    private boolean editable;
    private boolean blob;
    private boolean canFilter;
    private boolean required;
    private String referencedType;
    private String referencedTypeField;

    public Field(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public Integer getListId() {
        return listId;
    }

    public void setListId(Integer listId) {
        this.listId = listId;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isBlob() {
        return blob;
    }

    public void setBlob(boolean blob) {
        this.blob = blob;
    }

    public boolean isCanFilter() {
        return canFilter;
    }

    public void setCanFilter(boolean canFilter) {
        this.canFilter = canFilter;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Field cloneRelated(String relatedType) {
        Field field = new Field(relatedType + "." + getName(), getLabel());
        field.setBlob(isBlob());
        field.setRequired(isRequired());
        field.setCanFilter(isCanFilter());
        field.setEditable(isEditable());
        field.setClazz(getClazz());
        field.setListId(getListId());
        field.setReferencedTypeField(getReferencedTypeField());
        field.setReferencedType(getReferencedType());
        return field;
    }

    public String getRelatedType() {
        int p = name.indexOf(".");
        if(p < 0) {
            return null;
        } else {
            return name.substring(0, p);
        }
    }

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public String getValue() {
        return label;
    }

    public String getReferencedType() {
        return referencedType;
    }

    public String resolveReference(Entity entity) {
        if(referencedType != null) {
            return referencedType;
        } else if(referencedTypeField != null) {
            return entity.getPropertyValue(referencedTypeField);
        } else {
            return null;
        }
    }

    public void setReferencedType(String referencedType) {
        this.referencedType = referencedType;
    }

    public String getReferencedTypeField() {
        return referencedTypeField;
    }

    public void setReferencedTypeField(String referencedTypeField) {
        this.referencedTypeField = referencedTypeField;
    }

    /**
     * Field references one or more entity types.
     * @return reference
     */
    public boolean isReference() {
        return referencedType != null || referencedTypeField != null;
    }
}