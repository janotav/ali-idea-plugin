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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Relation {

    private final String name;
    private final String targetType;
    private final List<String> aliases;

    public Relation(String name, String targetType) {
        this.name = name;
        this.targetType = targetType;
        this.aliases = new ArrayList<String>();
    }

    public Relation(String targetType) {
        this.name = targetType;
        this.targetType = targetType;
        this.aliases = Arrays.asList(targetType);
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addAlias(String alias) {
        aliases.add(alias);
    }

    public String getTargetType() {
        return targetType;
    }

    public String getName() {
        return name;
    }
}
