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

public class EntityLoaded implements EntityListener {

    private Handler handler;
    private Listener listener;

    public EntityLoaded(Handler handler, Listener listener) {
        this.handler = handler;
        this.listener = listener;
    }

    public EntityLoaded(Handler handler, final String entityType, final int entityId, final Event event) {
        this(handler, entityType, entityId, event, null);
    }

    public EntityLoaded(Handler handler, final String entityType, final int entityId, final Event event, final Listener listener) {
        this.handler = handler;
        this.listener = new Listener() {
            @Override
            public void evaluate(Entity entity, Event pEvent) {
                Assert.assertEquals(entityType, entity.getType());
                Assert.assertEquals(entityId, entity.getId());
                Assert.assertEquals(event, pEvent);
                if(listener != null) {
                    listener.evaluate(entity, pEvent);
                }
            }
        };
    }

    @Override
    public void entityLoaded(final Entity entity, final Event event) {
        handler.done(new Runnable() {
            @Override
            public void run() {
                listener.evaluate(entity, event);
            }
        });
    }

    @Override
    public void entityNotFound(EntityRef ref, boolean removed) {
        handler.done(new Runnable() {
            @Override
            public void run() {
                Assert.fail("Not expected");
            }
        });
    }

    public interface Listener {

        void evaluate(Entity entity, Event event);

    }
}
