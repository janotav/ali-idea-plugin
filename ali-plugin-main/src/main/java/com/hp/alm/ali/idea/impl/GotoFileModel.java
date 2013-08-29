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

package com.hp.alm.ali.idea.impl;

import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.openapi.project.Project;

import java.lang.reflect.Constructor;

public class GotoFileModel {

    public static ChooseByNameModel getGotoFileModel(Project project) {
        try {
            Class<ChooseByNameModel> clazz = (Class)Class.forName("com.intellij.ide.util.gotoByName.GotoFileModel");
            Constructor<ChooseByNameModel > ctor = clazz.getConstructor(Project.class);
            return ctor.newInstance(project);
        } catch(Exception e) {
            return null;
        }
    }
}
