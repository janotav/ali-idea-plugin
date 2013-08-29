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

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.CachingEntityListener;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.model.parser.EntityList;
import com.intellij.openapi.project.Project;

public abstract class AbstractCachingEntityService<S> extends AbstractCachingService<S, EntityList, AbstractCachingService.Callback<EntityList>> implements CachingEntityListener {

    public AbstractCachingEntityService(Project project) {
        super(project);

        project.getComponent(EntityService.class).addEntityListener(this);
    }

    @Override
    public Entity lookup(final EntityRef ref) {
        synchronized (cache) {
            Entity entity = new Entity(ref.type, ref.id);
            for(EntityList list: cache.values()) {
                int i = list.indexOf(entity);
                if(i >= 0) {
                    return list.get(i);
                }
            }
        }
        return null;
    }

    @Override
    public void entityLoaded(Entity entity, Event event) {
    }

    @Override
    public void entityNotFound(EntityRef ref, boolean removed) {
    }
}
