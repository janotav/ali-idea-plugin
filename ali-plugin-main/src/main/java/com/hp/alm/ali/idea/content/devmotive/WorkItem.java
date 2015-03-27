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

package com.hp.alm.ali.idea.content.devmotive;

import com.hp.alm.ali.idea.entity.EntityRef;

final public class WorkItem {

    private static final WorkItem UNASSIGNED_INSTANCE = new WorkItem("Change not assigned to any work item");
    private static final WorkItem UNRESOLVED_INSTANCE = new WorkItem("Change not recognized");

    private final Type type;
    private final Integer id;
    private String name;

    public enum Type { USER_STORY, DEFECT, NONE }

    public static WorkItem unassigned() {
        return UNASSIGNED_INSTANCE;
    }

    public static WorkItem unresolved() {
        return UNRESOLVED_INSTANCE;
    }

    public WorkItem(String type, int id) {
        if ("requirement".equals(type)) {
            this.type = Type.USER_STORY;
        } else if ("defect".equals(type)) {
            this.type = Type.DEFECT;
        } else {
            throw new IllegalArgumentException();
        }
        this.id = id;
    }

    private WorkItem(String name) {
        type = Type.NONE;
        id = null;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityRef toEntityRef() {
        switch (type) {
            case DEFECT:
                return new EntityRef("defect", id);

            case USER_STORY:
                return new EntityRef("requirement", id);

            default:
                throw new UnsupportedOperationException();
        }
    }
}
