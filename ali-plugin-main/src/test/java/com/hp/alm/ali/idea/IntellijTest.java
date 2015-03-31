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

import com.hp.alm.ali.Handler;
import com.hp.alm.ali.Isolated;
import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.entity.EntityListener;
import com.hp.alm.ali.idea.rest.RestException;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.ErrorService;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.services.MetadataSimpleService;
import com.hp.alm.ali.idea.services.TestMessages;
import com.hp.alm.ali.idea.util.ApplicationUtil;
import com.hp.alm.ali.idea.util.DetailUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.LinkedList;
import java.util.List;

/**
 * To override the default Intellij logging configuration you need to:
 *
 * <pre>
 *   $ cd IDEA_HOME
 *   $ copy bin/log.xml test-log.xml
 * </pre>
 *
 * and edit test-log.xml to your liking.
 */
public abstract class IntellijTest {

    protected Handler handler;
    protected IdeaProjectTestFixture fixture;
    protected ServerVersion version;
    protected ErrorService errorService;
    protected EntityService entityService;
    private List<EntityListener> entityListeners;

    public IntellijTest(ServerVersion version) {
        this.version = version;
    }

    @Rule
    public TestName name = new TestName();

    @Rule
    public TestFailure failure = new TestFailure();

    protected TestMessages testMessages;
    protected TestApplication testApplication;

    @Before
    public void reset() throws Exception {
        if(isIsolated()) {
            fixture = FixtureFactory.createFixture(version);
            handler = FixtureFactory.createHandler(version, fixture);
        } else {
            fixture = FixtureFactory.ensureFixture(version);
            handler = FixtureFactory.ensureHandler(version, fixture);
            handler.clear();
        }

        errorService = getComponent(ErrorService.class);
        entityService = getComponent(EntityService.class);
        entityListeners = new LinkedList<EntityListener>();
        getComponent(MetadataService.class).connectedTo(ServerType.NONE);
        getComponent(MetadataSimpleService.class).connectedTo(ServerType.NONE);
        testMessages = new TestMessages();
        Messages.setTestDialog(testMessages);
        testApplication = new TestApplication();
        ApplicationUtil._setApplication(testApplication);
    }

    @After
    public void done() throws Throwable {
        testApplication.waitForBackgroundActivityToFinish();
        if(handler != null) {
            handler.finish();
        }
        Assert.assertTrue("Unhandled messages: " + testMessages.asString(), testMessages.isEmpty());
        Messages.setTestDialog(TestDialog.DEFAULT);
        List<Exception> ex = errorService._shiftErrors();
        Assert.assertTrue("Unhandled exceptions: " + ex.toString(), ex.isEmpty());
        for(EntityListener listener: entityListeners) {
            entityService.removeEntityListener(listener);
        }
        getComponent(DetailUtil.class)._restore();
        if(isIsolated()) {
            handler.getServer().stop();
        }
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        FixtureFactory.stopHandlers();
    }

    protected Project getProject() {
        return fixture.getProject();
    }

    protected <E> E getComponent(Class<E> clazz) {
        return getProject().getComponent(clazz);
    }

    protected void addEntityListener(EntityListener listener) {
        entityService.addEntityListener(listener);
        entityListeners.add(listener);
    }

    protected void checkError(String message) {
        Exception ex = errorService._shiftError();
        Assert.assertTrue(ex instanceof RestException);
        Assert.assertEquals(message, ex.getMessage());
    }

    private boolean isIsolated() throws NoSuchMethodException {
        Isolated isolated = getClass().getMethod(name.getMethodName()).getAnnotation(Isolated.class);
        if(isolated == null) {
            isolated = getClass().getAnnotation(Isolated.class);
        }
        return (isolated != null);
    }

    /**
     * TestName provided with JUnit fails to parse the [VERSION] prefix.
     */
    private static class TestName extends TestWatcher {
        private String fName;

        @Override
        protected void starting(Description d) {
            fName = d.toString().replaceFirst("^(?:\\[.*\\] )?(.*?)\\(.*\\)$", "$1");
        }

        public String getMethodName() {
            return fName;
        }
    }

    private class TestFailure extends TestWatcher {

        @Override
        protected void failed(Throwable t, Description description) {
            try {
                FixtureFactory.removeFixture(version);
                FixtureFactory.removeHandler(version);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
