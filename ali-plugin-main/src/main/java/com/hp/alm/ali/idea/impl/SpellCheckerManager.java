/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.impl;

import com.intellij.openapi.project.Project;

import java.lang.reflect.Method;

public class SpellCheckerManager {

    private Method hasProblem;
    private Object spellCheckerManagerInstance;

    public SpellCheckerManager(Project project) {
        try {
            Class<?> clazz = Class.forName("com.intellij.spellchecker.SpellCheckerManager");
            Method getInstance = clazz.getMethod("getInstance", Project.class);
            spellCheckerManagerInstance = getInstance.invoke(null, project);
            hasProblem = clazz.getMethod("hasProblem", String.class);
        } catch (Exception e) {
            // spell check not available
        }
    }

    public boolean hasProblem(String word) {
        try {
            if (hasProblem != null) {
                return (Boolean) hasProblem.invoke(spellCheckerManagerInstance, word);
            }
        } catch (Exception e) {
            // spell check not available
        }
        return false;
    }

    public static boolean isAvailable() {
        try {
            Class<?> clazz = Class.forName("com.intellij.spellchecker.SpellCheckerManager");
            return clazz.getMethod("getInstance", Project.class) != null &&
                    clazz.getMethod("hasProblem", String.class) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
