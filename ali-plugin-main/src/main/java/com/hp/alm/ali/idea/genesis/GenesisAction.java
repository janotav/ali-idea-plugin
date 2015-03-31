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

package com.hp.alm.ali.idea.genesis;

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.genesis.checkout.Checkout;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;

import java.io.File;

public class GenesisAction extends AnAction {

    public void actionPerformed(AnActionEvent anActionEvent) {
        final GenesisDialog genesis = new GenesisDialog();
        genesis.show();
        if(genesis.isOK()) {
            Checkout checkout = genesis.getCheckout();
            if(checkout != null) {
                VcsConfiguration configuration = VcsConfiguration.getInstance(ProjectManager.getInstance().getDefaultProject());
                configuration.PERFORM_CHECKOUT_IN_BACKGROUND = false;
                Project p = ProjectManager.getInstance().getDefaultProject();
                checkout.setTarget(genesis.getTarget());
                checkout.doCheckout(p, new CheckoutProvider.Listener() {
                    // 11.0.1
                    public void directoryCheckedOut(File file, VcsKey vcsKey) {
                    }

                    // 10.5.4
                    public void directoryCheckedOut(File file) {
                    }

                    public void checkoutCompleted() {
                        ProjectManagerAdapter adapter = new ProjectManagerAdapter() {
                            public void projectOpened(Project project) {
                                AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
                                conf.ALM_LOCATION = genesis.getAlmLocation();
                                conf.ALM_PROJECT = genesis.getProject();
                                conf.ALM_DOMAIN = genesis.getDomain();
                                conf.ALM_USERNAME = genesis.getUsername();
                                conf.ALM_PASSWORD = genesis.getPassword();
                            }
                        };

                        ProjectManager.getInstance().addProjectManagerListener(adapter);
                        try {
                            VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(genesis.getTarget()));
                            AddModuleWizard wizard = ImportModuleAction.createImportWizard(null, null, file, ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions());
                            NewProjectUtil.createNewProject(null, wizard);
                        } catch (Exception e) {
                            Messages.showErrorDialog("Failed to complete the operation. Please invoke the project wizard manually.\nSources were " +
                                    "checked out to the following location:\n\n " + genesis.getTarget(), "Operation Failed");
                        }
                        ProjectManager.getInstance().removeProjectManagerListener(adapter);
                    }
                });
            }
        }
    }
}
