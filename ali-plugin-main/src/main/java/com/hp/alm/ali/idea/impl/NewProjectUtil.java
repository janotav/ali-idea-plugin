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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.lang.reflect.Method;

public class NewProjectUtil {

    public static void createNewProject(Project projectToClose, String defaultPath) {
        try {
            Class<?> clazz = Class.forName("com.intellij.ide.impl.NewProjectUtil");
            Method method = clazz.getMethod("createNewProject", Project.class, String.class);
            method.invoke(null, projectToClose, defaultPath);
        } catch(Exception e) {
            Messages.showErrorDialog("Failed to complete the operation. Please invoke the project wizard manually.\nSources were " +
                    "checked out to the following location:\n\n "+defaultPath, "Operation failed");
        }
    }
}
