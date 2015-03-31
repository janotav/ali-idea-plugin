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

package com.hp.alm.ali.idea;

import com.hp.alm.ali.idea.util.ApplicationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import org.junit.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class TestApplication implements ApplicationUtil.Application {

    private static final int MAX = 10000;

    private Semaphore semaphore = new Semaphore(MAX);

    @Override
    public void invokeLater(final Runnable runnable) {
        Assert.assertTrue(semaphore.tryAcquire());
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    semaphore.release();
                }
            }
        });
    }

    @Override
    public void executeOnPooledThread(final Runnable runnable) {
        Assert.assertTrue(semaphore.tryAcquire());
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    semaphore.release();
                }
            }
        });
    }

    @Override
    public <T> Future<T> executeOnPooledThread(final Callable<T> callable) {
        Assert.assertTrue(semaphore.tryAcquire());
        return ApplicationManager.getApplication().executeOnPooledThread(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    return callable.call();
                } finally {
                    semaphore.release();
                }
            }
        });
    }

    @Override
    public void invokeLaterIfNeeded(final Runnable runnable) {
        Assert.assertTrue(semaphore.tryAcquire());
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    semaphore.release();
                }
            }
        });
    }

    public void waitForBackgroundActivityToFinish() {
        semaphore.acquireUninterruptibly(MAX);
        semaphore.release(MAX);
    }
}
