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

package com.hp.alm.ali.idea.entity;

import com.hp.alm.ali.idea.model.Entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SimpleCache {

    private List<Entity> list = new LinkedList<Entity>();
    private Set<EntityRef> notFound = new HashSet<EntityRef>();

    public synchronized Entity lookup(EntityRef ref, Collection<String> props) {
        for(Entity entity: list) {
            if(new EntityRef(entity).equals(ref)) {
                for(String prop: props) {
                    if(!entity.isInitialized(prop)) {
                        return null;
                    }
                }
                return entity;
            }
        }
        return null;
    }

    public synchronized void add(Entity entity) {
        if(!notFound.remove(new EntityRef(entity.getType(), entity.getId()))) {
            list.remove(entity);
        }
        list.add(0, entity);
    }

    public synchronized void addNotFound(EntityRef ref) {
        notFound.add(ref);
        list.remove(new Entity(ref.type, ref.id));
    }

    public synchronized boolean isNotFound(EntityRef ref) {
        return notFound.contains(ref);
    }

    public synchronized void clear() {
        list.clear();
        notFound.clear();
    }
}
