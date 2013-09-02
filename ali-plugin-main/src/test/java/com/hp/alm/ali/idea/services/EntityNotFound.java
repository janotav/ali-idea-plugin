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

import com.hp.alm.ali.Handler;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import org.junit.Assert;

public class EntityNotFound implements EntityListener {

    private Handler handler;
    private final String entityType;
    private final int entityId;
    private final boolean removed;

    public EntityNotFound(Handler handler, String entityType, int entityId, boolean removed) {
        this.handler = handler;
        this.entityType = entityType;
        this.entityId = entityId;
        this.removed = removed;
    }

    @Override
    public void entityLoaded(final Entity entity, final Event event) {
        handler.done(new Runnable() {
            @Override
            public void run() {
                Assert.fail("Not expected");
            }
        });
    }

    @Override
    public void entityNotFound(final EntityRef ref, final boolean removed) {
        handler.done(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(entityType, ref.type);
                Assert.assertEquals(entityId, ref.id);
                Assert.assertEquals(EntityNotFound.this.removed, removed);
            }
        });
    }
}
