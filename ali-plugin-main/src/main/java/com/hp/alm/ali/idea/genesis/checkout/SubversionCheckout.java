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

package com.hp.alm.ali.idea.genesis.checkout;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.impl.SvnCheckoutProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import org.jetbrains.idea.svn.api.Depth;
import org.jetbrains.idea.svn.api.Revision;

import java.io.File;

public class SubversionCheckout implements Checkout {
    private String path;
    private Entity branch;
    private Entity repo;

    public void setRepository(Entity repo) {
        this.repo = repo;
    }

    public void setBranch(Entity branch) {
        this.branch = branch;
    }

    public void setTarget(String path) {
        this.path = path;
    }

    public String doCheckout(Project project, CheckoutProvider.Listener listener) {
        String url = String.valueOf(repo.getProperty("location")) + branch.getProperty("path");
        File projectDir = new File(path);
        SvnCheckoutProvider.doCheckout(project, projectDir, url, Revision.HEAD, Depth.INFINITY, false, listener);
        return path;
    }
}
