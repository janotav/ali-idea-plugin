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

package com.hp.alm.ali.idea.genesis;

import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;

import java.io.File;

public class ProjectImpl implements ProjectApi {

    @Override
    public void createNewProject(Project projectToClose, String defaultPath) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(defaultPath));
        AddModuleWizard wizard = ImportModuleAction.createImportWizard(null, null, file, ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions());
        NewProjectUtil.createNewProject(projectToClose, wizard);
    }
}
