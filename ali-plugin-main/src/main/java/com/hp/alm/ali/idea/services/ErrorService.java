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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.idea.ui.dialog.RestErrorDetailDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.TestOnly;

import java.util.LinkedList;
import java.util.List;

public class ErrorService {

    private Project project;
    private LinkedList<Exception> errors;

    public ErrorService(Project project) {
        this.project = project;
        errors = new LinkedList<Exception>();
    }

    public void showException(Exception e) {
        if(ApplicationManager.getApplication().isUnitTestMode()) {
            errors.add(e);
        } else {
            new RestErrorDetailDialog(project, e).setVisible(true);
        }
    }

    @TestOnly
    public Exception _shiftError() {
        if(!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This method is only applicable in the context of unit tests");
        } else {
            return errors.removeFirst();
        }
    }

    @TestOnly
    public List<Exception> _shiftErrors() {
        if(!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This method is only applicable in the context of unit tests");
        } else {
            List<Exception> ret = errors;
            this.errors = new LinkedList<Exception>();
            return ret;
        }
    }
}
