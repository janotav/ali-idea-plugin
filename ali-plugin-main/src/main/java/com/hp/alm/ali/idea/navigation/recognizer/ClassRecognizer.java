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

package com.hp.alm.ali.idea.navigation.recognizer;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.navigation.Recognizer;
import com.hp.alm.ali.idea.util.EditSourceUtil;
import com.hp.alm.ali.idea.util.FileEditorManager;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.HashMap;
import java.util.Map;

public abstract class ClassRecognizer implements Recognizer {

    public boolean navigate(Project project, String hyperlink)  {
        if(hyperlink.startsWith("goto:")) {
            openEditor(project, hyperlink.substring(5));
            return true;
        } else {
            return false;
        }
    }

    private static Editor openEditor(Project project, String link) {
        return evaluate(project, link, true);
    }

    private static Editor evaluate(Project project, String link, boolean navigate) {
        Map<String, String> query = parseQuery(link);
        String className = query.get("c");
        int line = query.containsKey("l")? Integer.valueOf(query.get("l")): 0;
        String file = query.get("f");
        String methodName = query.get("m");
        for(ChooseByNameContributor contributor: ChooseByNameRegistry.getInstance().getClassModelContributors()) {
            NavigationItem[] byName = contributor.getItemsByName(file, file, project, true);
            NavigationItem item = matchClass(byName, className);
            if(item != null) {
                if(!navigate) {
                    return null;
                }
                if(item instanceof PsiClass && methodName != null) {
                    for(PsiMethod method: ((PsiClass)item).getMethods()) {
                        if(method.getName().equals(methodName)) {
                            project.getComponent(EditSourceUtil.class).navigate(method, true, true);
                            return project.getComponent(FileEditorManager.class).getSelectedTextEditor();
                        }
                    }
                }
                project.getComponent(EditSourceUtil.class).navigate(item, true, true);
                Editor editor = project.getComponent(FileEditorManager.class).getSelectedTextEditor();
                if(line > 0) {
                    if(editor != null /* && !editor.getDocument().getText().startsWith(PsiBundle.message("psi.decompiled.text.header")) */) { // TODO: string missing since commit 02ca2bd104e91420543cf8c19b2c6a9cbb6c5ab7
                        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line - 1, 0));
                        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                        editor.getSelectionModel().removeSelection();
                    }
                }
                return editor;
            }
        }
        return null;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> ret = new HashMap<String, String>();
        for(String pair: query.split("&")) {
            int p = pair.indexOf('=');
            ret.put(pair.substring(0, p), EntityQuery.decode(pair.substring(p + 1)));
        }
        return ret;
    }

    private static NavigationItem matchClass(NavigationItem[] items, String className) {
        for(NavigationItem item: items) {
            if(item instanceof PsiClass) {
                if(className.equals(((PsiClass) item).getQualifiedName())) {
                    return item;
                }
            }
        }
        if(items.length > 0) {
            return items[0];
        } else {
            return null;
        }
    }
}
