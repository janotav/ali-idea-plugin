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

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.TestTarget;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.Arrays;

public class MultiTestRunner extends BlockJUnit4ClassRunner {

    private ServerVersion version;

    public MultiTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        Description description = Description.createSuiteDescription(
                testName(method),
                method.getAnnotations());

        TestTarget testTarget = method.getMethod().getAnnotation(TestTarget.class);
        if(testTarget == null) {
            testTarget = method.getMethod().getDeclaringClass().getAnnotation(TestTarget.class);
        }

        for (ServerVersion version: ServerVersion.values()) {
            if(testTarget != null && !Arrays.asList(testTarget.value()).contains(version)) {
                continue;
            }
            description.addChild(Description.createTestDescription(
                    getTestClass().getJavaClass(),
                    "[" + version + "] " + testName(method)));
        }
        return description;
    }

    protected Object createTest() throws Exception {
        Object testInstance = getTestClass().getOnlyConstructor().newInstance();
        ((MultiTest)testInstance).setServerVersion(version);
        return testInstance;
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (method.getAnnotation(Ignore.class) != null) {
            notifier.fireTestIgnored(description);
        } else {
            for (Description desc : description.getChildren()) {
                String serverString = desc.getDisplayName().replaceFirst("\\[(.*)\\].*", "$1");
                this.version = ServerVersion.valueOf(serverString);
                runLeaf(methodBlock(method), desc, notifier);
            }
        }
    }
}
