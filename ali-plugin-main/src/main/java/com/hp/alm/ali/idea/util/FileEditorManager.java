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

package com.hp.alm.ali.idea.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.TestOnly;

public class FileEditorManager {

    private Selector selector = new DefaultSelector();
    private Project project;

    public FileEditorManager(Project project) {
        this.project = project;
    }

    public Editor getSelectedTextEditor() {
        return selector.getSelectedTextEditor();
    }

    @TestOnly
    public void _setSelector(Selector selector) {
        this.selector = selector;
    }

    @TestOnly
    public void _restore() {
        selector = new DefaultSelector();
    }

    public static interface Selector {

        public Editor getSelectedTextEditor();

    }

    private class DefaultSelector implements Selector {

        @Override
        public Editor getSelectedTextEditor() {
            return com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).getSelectedTextEditor();
        }
    }
}
