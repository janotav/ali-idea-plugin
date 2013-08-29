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

package com.hp.alm.ali.idea.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepositoryType;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.util.Consumer;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class HpAlmRepositoryType extends TaskRepositoryType<HpAlmRepository> {

    public String getName() {
        return "HP_ALM";
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/ali_icon.png");
    }

    public HpAlmRepository createRepository() {
        return new HpAlmRepository();
    }

    public Class<HpAlmRepository> getRepositoryClass() {
        return HpAlmRepository.class;
    }

    public TaskRepositoryEditor createEditor(HpAlmRepository repository, Project project, Consumer<HpAlmRepository> changeListener) {
        return new HpAlmRepositoryEditor(project, repository);
    }
}
