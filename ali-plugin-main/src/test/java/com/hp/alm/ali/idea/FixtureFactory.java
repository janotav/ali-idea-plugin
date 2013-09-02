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
import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.SprintService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.util.ui.UIUtil;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FixtureFactory {

    private static final Logger logger = Logger.getLogger(FixtureFactory.class);

    private static Map<ServerVersion, IdeaProjectTestFixture> fixtures = new HashMap<ServerVersion, IdeaProjectTestFixture>();
    private static Map<ServerVersion, Handler> handlers = new HashMap<ServerVersion, Handler>();
    private static Throwable failed;

    public static IdeaProjectTestFixture createFixture(final ServerVersion version) {
        logger.info("Setting up Intellij fixture for " + version);

        final Ref<IdeaProjectTestFixture> fixture = new Ref<IdeaProjectTestFixture>();
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    IdeaProjectTestFixture myFixture = JavaTestFixtureFactory.createFixtureBuilder(version.name() + System.currentTimeMillis()).getFixture();
                    myFixture.setUp();
                    fixture.set(myFixture);
                } catch (Throwable t) {
                    failed = t;
                }
            }
        });
        return fixture.get();
    }

    public static IdeaProjectTestFixture ensureFixture(ServerVersion version) {
        if(!fixtures.containsKey(version)) {
            fixtures.put(version, createFixture(version));
            if(failed != null) {
                throw new RuntimeException(failed);
            }
        }
        return fixtures.get(version);
    }

    public static void removeFixture(ServerVersion version) {
        fixtures.remove(version);
    }

    public static void handshake(Handler handler, boolean auth) {
        ServerVersion version = handler.getVersion();

        if(auth) {
            handler.authenticate();
        }

        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/defects?query={id[0]}", 200);

        switch (version) {
            case AGM:
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/../../../../?alt=application/atomsvc+xml", 403);
                handler.authenticate(); // client will try the request again
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/../../../../?alt=application/atomsvc+xml", 403);

                RestInvocations.sprintService_getReleases(handler);
                break;

            case ALM12:
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/../../../../?alt=application/atomsvc+xml", 200)
                        .content("rest.xml");
                break;

            case ALI:
            case ALI2:
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/../../../../?alt=application/atomsvc+xml", 200)
                        .content("rest.xml");
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/changesets?query={id[0]}", 200)
                        .content("no_entities.xml");
                break;

            case ALM11:
                handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/../../../../?alt=application/atomsvc+xml", 200)
                        .content("rest.xml");
                break;
        }
    }

    public static Handler createHandler(ServerVersion version, IdeaProjectTestFixture fixture) throws Exception {
        logger.info("Setting up REST handler for " + version);

        Server server = new Server(0);
        Handler handler = new Handler(version);
        server.setHandler(handler);
        server.start();

        handshake(handler, true);

        final Semaphore semaphore = new Semaphore(0);
        Project project = fixture.getProject();
        final RestService restService = project.getComponent(RestService.class);
        restService.addServerTypeListener(new ServerTypeListener() {
            @Override
            public void connectedTo(ServerType serverType) {
                if (serverType.isConnected()) {
                    semaphore.release(1);
                    restService.removeServerTypeListener(this);
                }
            }
        }, false);

        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
        conf.ALM_LOCATION = handler.getQcUrl();
        conf.ALM_DOMAIN = "domain";
        conf.ALM_PROJECT = "project";
        conf.ALM_USERNAME = "user";
        conf.ALM_PASSWORD = "password";
        conf.fireChanged();

        handler.consume();

        try {
            Assert.assertTrue(semaphore.tryAcquire(1, 2000, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(version == ServerVersion.AGM) {
            // wait for releases to be fetched
            project.getComponent(SprintService.class).getReleases();
        }

        return handler;
    }

    public static Handler ensureHandler(ServerVersion version, IdeaProjectTestFixture fixture) throws Exception {
        Handler handler;
        if(!handlers.containsKey(version)) {
            handler = createHandler(version, fixture);
            handlers.put(version, handler);
        } else {
            handler = handlers.get(version);
            handler.getServer().start();
            return handler;
        }
        return handler;
    }

    public static void stopHandlers() throws Exception {
        for(Handler handler: handlers.values())  {
            if(handler.getServer().isStarted()) {
                Server newServer = new Server(handler.getLocalPort());
                handler.getServer().stop();
                newServer.setHandler(handler);
            }
        }
    }

    public static void removeHandler(ServerVersion version) throws Exception {
        Handler handler = handlers.remove(version);
        handler.getServer().stop();
    }
}
