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

import com.hp.alm.ali.idea.action.devmotive.DevMotiveGutterAction;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;

public class DevMotiveAnnotationGutterActionProvider implements AnnotationGutterActionProvider {

    @NotNull
    @Override
    public AnAction createAction(final FileAnnotation annotation) {
        AliConfiguration aliConfiguration = ApplicationManager.getApplication().getComponent(AliConfiguration.class);
        if (aliConfiguration.devMotiveAnnotation) {
            try {
                Field myProject = annotation.getClass().getDeclaredField("myProject");
                myProject.setAccessible(true);
                final Project project = (Project) myProject.get(annotation);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        DevMotiveGutterAction.registerDevMotiveTextAnnotation(project, annotation);
                    }
                });
                return EmptyAction.createEmptyAction(null, null, false);
            } catch (Exception e) {
                // if anything goes wrong, simply fallback to the regular action
            }
        }
        return new DevMotiveGutterAction(annotation);
    }
}
