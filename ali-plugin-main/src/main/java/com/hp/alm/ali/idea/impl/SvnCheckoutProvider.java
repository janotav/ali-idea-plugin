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
import com.intellij.openapi.vcs.CheckoutProvider;
import org.jetbrains.idea.svn.api.Depth;
import org.jetbrains.idea.svn.api.Revision;

import java.io.File;
import java.lang.reflect.Method;

public class SvnCheckoutProvider {

    public static void doCheckout(Project project, File target, String url, Revision revision,
                                    Depth depth, boolean ignoreExternals, CheckoutProvider.Listener listener) {
        try {
            Class<?> clazz = Class.forName("org.jetbrains.idea.svn.checkout.SvnCheckoutProvider");
            Method method = clazz.getMethod("doCheckout", Project.class, File.class, String.class, Revision.class, Depth.class, boolean.class, CheckoutProvider.Listener.class);
            method.invoke(null, project, target, url, revision, depth, ignoreExternals, listener);
        } catch(Exception e) {
            Messages.showErrorDialog("Failed to complete the operation. Please checkout the source code from the following subversion repository and invoke the project wizard manually:\n\n "+
                    "SVN URL: "+url, "Operation failed");
        }
    }
}
