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

package com.hp.alm.ali;

import com.intellij.util.ui.UIUtil;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;

public class IntegrationTestRunner extends SpringJUnit4ClassRunner {

    private ServerVersion executionVersion;

    public IntegrationTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        executionVersion = TestSettings.getExecutionVersion();
    }

    @Override
    protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
        if(super.isTestMethodIgnored(frameworkMethod)) {
            return true;
        }
        TestTarget testTarget = frameworkMethod.getMethod().getAnnotation(TestTarget.class);
        if(testTarget == null) {
            testTarget = frameworkMethod.getClass().getAnnotation(TestTarget.class);
        }
        return testTarget != null && !ObjectUtils.containsElement(testTarget.value(), executionVersion);
    }

    public void run(final RunNotifier notifier) {
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                IntegrationTestRunner.super.run(notifier);
            }
        });
    }
}
