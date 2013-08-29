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

import com.hp.alm.ali.idea.rest.RestListener;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.Pair;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class AbstractCachingService<S, T, C extends AbstractCachingService.Callback<T>>  implements RestListener {

    private Map<S, Pair<List<C>, Future<T>>> active;
    private Map<S, RuntimeException> failures;
    final protected Map<S, T> cache;
    final protected Project project;

    public AbstractCachingService(Project project) {
        this.project = project;
        active = new HashMap<S, Pair<List<C>, Future<T>>>();
        cache = new HashMap<S, T>();
        failures = new HashMap<S, RuntimeException>();

        project.getComponent(RestService.class).addListener(this);
    }

    protected abstract T doGetValue(S key);

    @Override
    public void restConfigurationChanged() {
        synchronized (cache) {
            cache.clear();
            failures.clear();
        }
    }

    protected T getCachedValue(S key) {
        synchronized (cache) {
            T value = cache.get(key);
            if(value != null) {
                return value;
            }
            return null;
        }
    }

    protected T getValue(S key) {
        Future<T> future;
        synchronized (cache) {
            T value = cache.get(key);
            if(value != null) {
                return value;
            }
            RuntimeException ex = failures.get(key);
            if(ex != null) {
                throw ex;
            }
            future = getFuture(key, null, true);
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void getValueAsync(S key, C callback) {
        synchronized (cache) {
            T value = cache.get(key);
            if(value != null) {
                invoke(new LinkedList<C>(Collections.singletonList(callback)), value);
                return;
            }
            RuntimeException ex = failures.get(key);
            if(ex != null) {
                failureInvoke(new LinkedList<C>(Collections.singletonList(callback)));
                return;
            }
            getFuture(key, callback, false);
        }
    }

    private Future<T> getFuture(final S key, C callback, final boolean throwException) {
        Pair<List<C>, Future<T>> listAndFuture = active.get(key);
        if(listAndFuture == null) {
            Future<T> future = ApplicationManager.getApplication().executeOnPooledThread(new Callable<T>() {
                public T call() {
                    final T value;
                    try {
                        value = doGetValue(key);
                    } catch(RuntimeException e) {
                        synchronized (cache) {
                            List<C> listeners = active.get(key).getFirst();
                            failures.put(key, e);
                            failureInvoke(listeners);
                            active.remove(key);
                            if(throwException) {
                                throw e;
                            } else {
                                return null;
                            }
                        }
                    }
                    synchronized (cache) {
                        cache.put(key, value);

                        List<C> listeners = active.get(key).getFirst();
                        invoke(listeners, value);
                        active.remove(key);
                    }
                    return value;
                }
            });
            listAndFuture = new Pair<List<C>, Future<T>>(new LinkedList<C>(), future);
            active.put(key, listAndFuture);

        }
        if(callback != null) {
            listAndFuture.getFirst().add(callback);
        }
        return listAndFuture.getSecond();
    }

    private void invoke(final List<C> listeners, final T value) {
        for (Iterator<C> it = listeners.iterator(); it.hasNext(); ) {
            C listener = it.next();
            if(!(listener instanceof Dispatch)) {
                listener.loaded(value);
                it.remove();
            }
        }
        if(!listeners.isEmpty()) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    for (C listener : listeners) {
                        listener.loaded(value);
                    }
                }
            });
        }
    }

    private void failureInvoke(final List<C> listeners) {
        for (Iterator<C> it = listeners.iterator(); it.hasNext(); ) {
            C listener = it.next();
            if(!(listener instanceof FailureCallback)) {
                it.remove();
            } else if(!(listener instanceof Dispatch)) {
                ((FailureCallback)listener).failed();
                it.remove();
            }
        }
        if(!listeners.isEmpty()) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    for (C listener : listeners) {
                        ((FailureCallback)listener).failed();
                    }
                }
            });
        }
    }

    public static interface Dispatch {
    }

    public static interface FailureCallback {

        void failed();

    }

    public static interface Callback<E> {

        void loaded(E data);

    }

    public static interface DispatchCallback<E> extends Callback<E>, Dispatch {
    }
}
