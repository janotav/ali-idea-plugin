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
import org.junit.Assert;

import javax.swing.SwingUtilities;

public abstract class NonDispatchTestCallback<E> implements AbstractCachingService.Callback<E> {

    private Handler handler;

    public NonDispatchTestCallback(Handler handler) {
        this.handler = handler;
    }

    protected abstract void evaluate(final E data);

    @Override
    final public void loaded(final E data) {
        handler.done(new Runnable() {
            @Override
            public void run() {
                evaluate(data);
                Assert.assertFalse("Callback inside dispatch thread", SwingUtilities.isEventDispatchThread());
            }
        });
    }
}
