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

package com.hp.alm.ali.idea.entity;

import com.hp.alm.ali.idea.model.Entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class EntityRef {
    private static final Pattern PATTERN = Pattern.compile("^([\\w-]+) #(\\d+)$");

    public String type;
    public int id;

    public EntityRef(String type, int id) {
        this.type = type;
        this.id = id;
    }

    public EntityRef(Entity entity) {
        this.type = entity.getType();
        this.id = entity.getId();
    }

    public EntityRef(String taskName) {
        Matcher matcher = PATTERN.matcher(taskName);
        if(matcher.matches()) {
            this.type = matcher.group(1);
            this.id = Integer.valueOf(matcher.group(2));
        } else {
            throw new IllegalArgumentException(taskName);
        }
    }

    public Entity toEntity() {
        return new Entity(type, id);
    }

    public String toString() {
        return type + " #" +id;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return  true;
        } else if(!(o instanceof EntityRef)) {
            return false;
        } else {
            EntityRef other = (EntityRef) o;
            return other.type.equals(type) && other.id == id;
        }
    }

    public int hashCode() {
        return type.hashCode() * 31 + id;
    }

    public static boolean isEntityRef(String str) {
        Matcher matcher = PATTERN.matcher(str);
        return matcher.matches();
    }
}
