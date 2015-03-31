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

package com.hp.alm.ali.idea.util;

import com.hp.alm.ali.idea.content.AliContentFactory;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.TestOnly;

public class DetailUtil {

    private Launcher launcher = new DefaultLauncher();

    public void loadDetail(Project project, Entity entity, boolean show, boolean select) {
        launcher.loadDetail(project, entity, show, select);
    }

    @TestOnly
    public void _setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    @TestOnly
    public void _restore() {
        this.launcher = new DefaultLauncher();
    }

    public static interface Launcher {

        void loadDetail(Project project, Entity entity, boolean show, boolean select);

    }

    private static class DefaultLauncher implements Launcher {
        @Override
        public void loadDetail(Project project, Entity entity, boolean show, boolean select) {
            AliContentFactory.loadDetail(project, entity, show, select);
        }
    }
}
