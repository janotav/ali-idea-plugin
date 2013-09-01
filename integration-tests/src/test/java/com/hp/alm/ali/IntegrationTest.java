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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;

import java.util.concurrent.Semaphore;

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
@RunWith(IntegrationTestRunner.class)
@TestExecutionListeners({})
public abstract class IntegrationTest {

    private static final Logger logger = Logger.getLogger(IntegrationTest.class);

    protected IdeaProjectTestFixture myFixture;

    @Before
    final public void setUp() throws Exception {
        myFixture = JavaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder().getFixture();
        myFixture.setUp();

        final RestService restService = myFixture.getProject().getComponent(RestService.class);
        if(!restService.getServerTypeIfAvailable().isConnected()) {
            final Semaphore available = new Semaphore(0, true);
            restService.addServerTypeListener(new ServerTypeListener() {
                @Override
                public void connectedTo(ServerType serverType) {
                    if(serverType.isConnected()) {
                        available.release(1);
                        restService.removeServerTypeListener(this);
                    }
                }
            });

            AliProjectConfiguration conf = myFixture.getProject().getComponent(AliProjectConfiguration.class);
            conf.ALM_LOCATION = TestSettings.getServerUrl();
            conf.ALM_DOMAIN = TestSettings.getDomain();
            conf.ALM_PROJECT = TestSettings.getProject();
            conf.ALM_USERNAME = TestSettings.getUsername();
            conf.ALM_PASSWORD = TestSettings.getPassword();
            conf.fireChanged();

            logger.info("Waiting for project/tenant connection...");
            available.acquire();
            logger.info("Connected");
        }
    }
}
