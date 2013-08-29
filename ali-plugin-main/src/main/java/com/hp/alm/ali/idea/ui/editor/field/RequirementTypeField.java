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

import com.hp.alm.ali.idea.model.parser.RequirementTypeList;
import com.hp.alm.ali.idea.services.RequirementTypeService;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.Field;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RequirementTypeField extends AbstractListField {

    private Map<String, Integer> map = new HashMap<String, Integer>();
    private Map<Integer, String> mapInv = new HashMap<Integer, String>();

    public RequirementTypeField(Project project, Field field, Entity entity, boolean editable) {
        super(field.getLabel(), field.isRequired(), editable);

        RequirementTypeList requirementTypes = project.getComponent(RequirementTypeService.class).getRequirementTypes();

        map = new HashMap<String, Integer>();
        mapInv = new HashMap<Integer, String>();
        for(Entity ref: requirementTypes) {
            map.put(ref.getPropertyValue("name"), ref.getId());
            mapInv.put(ref.getId(), ref.getPropertyValue("name"));
        }

        LinkedList<String> options = new LinkedList<String>(map.keySet());
        initializeValues(options, mapInv.get(Integer.valueOf(entity.getPropertyValue(field.getName()))));
    }

    public String getValue() {
        return getTheValue(getSelectedItem());
    }

    public boolean hasChanged() {
        return !getValue().equals(getTheValue(getOriginalValue()));
    }

    private String getTheValue(String item) {
        Integer id = map.get(item);
        return id == null? "": String.valueOf(id);
    }
}
