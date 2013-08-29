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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WeakListeners<E> {

    final private List<MyReference<E>> listeners = new LinkedList<MyReference<E>>();

    public void fire(Action<E> action) {
        List<E> copy = new LinkedList<E>();
        synchronized(listeners) {
            for(Iterator<MyReference<E>> it = listeners.iterator(); it.hasNext(); ) {
                E theListener = it.next().get();
                if(theListener != null) {
                    copy.add(theListener);
                } else {
                    it.remove();
                }
            }
        }
        for(E listener: copy) {
            action.fire(listener);
        }
    }

    public void add(E listener, boolean weak) {
        synchronized (listeners) {
            if(weak) {
                listeners.add(new MyWeakReference<E>(listener));
            } else {
                listeners.add(new MyStrongReference<E>(listener));
            }
        }
    }

    public void add(E listener) {
        add(listener, true);
    }

    public void remove(E listener) {
        synchronized (listeners) {
            for(Iterator<MyReference<E>> it = listeners.iterator(); it.hasNext(); ) {
                E theListener = it.next().get();
                if(theListener == null || theListener.equals(listener)) {
                    it.remove();
                }
            }
        }
    }

    public static interface Action<E> {

        void fire(E listener);

    }

    private static interface MyReference<E> {

        E get();

    }

    private static class MyWeakReference<E> extends WeakReference<E> implements MyReference<E> {

        public MyWeakReference(E referent) {
            super(referent);
        }
    }

    private static class MyStrongReference<E> implements MyReference<E> {

        private E referent;

        public MyStrongReference(E referent) {
            this.referent = referent;
        }

        @Override
        public E get() {
            return referent;
        }
    }
}
