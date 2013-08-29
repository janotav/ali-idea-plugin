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

package com.hp.alm.ali.idea.entity.edit;

import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import java.util.HashSet;
import java.util.Set;

public class MayaLock extends DummyLock {
    /*
     * List of entity types that do not support locking.
     */
    private static Set<String> blacklist;
    static  {
        blacklist = new HashSet<String>();
        blacklist.add("build-instance");
        blacklist.add("defect-link");
        blacklist.add("attachment");
    }

    public MayaLock(Project project) {
        super(project);
    }

    @Override
    public Entity lock(Entity entity) {
        if(!blacklist.contains(entity.getType())) {
            return entityService.lockEntity(entity, false);
        } else {
            return super.lock(entity);
        }
    }

    @Override
    public void unlock(Entity entity) {
        if(!blacklist.contains(entity.getType())) {
            entityService.unlockEntity(entity);
        } else {
            super.unlock(entity);
        }
    }
}
