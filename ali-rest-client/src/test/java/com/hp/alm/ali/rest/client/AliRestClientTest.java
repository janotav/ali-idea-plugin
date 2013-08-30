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

package com.hp.alm.ali.rest.client;

import junit.framework.Assert;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AliRestClientTest {

    private static Server server;
    private static Handler handler;

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);
        handler = new Handler();
        server.setHandler(handler);
        server.start();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
    }

    @Before
    public void reset() {
        handler.clear();
    }

    @After
    public void done() {
        handler.done();
    }

    public static String getQcUrl() {
        return getServerUrl("/qcbin");
    }

    public static String getServerUrl(String path) {
        return "http://localhost:"+((ServerConnector)server.getConnectors()[0]).getLocalPort() + path;
    }

    @Test
    public void testRequireDomainWhenProjectSpecified_create() {
        try {
            AliRestClient.create("http://location", null, "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
            Assert.fail("Domain is mandatory when project is specified.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRequireDomainWhenProjectSpecified_setDomain() {
        AliRestClient client = AliRestClient.create("http://location", "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.setDomain(null);
            Assert.fail("Domain is mandatory when project is specified.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRequireDomainWhenProjectSpecified_setProject() {
        AliRestClient client = AliRestClient.create("http://location", null, null, "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.setProject("foo");
            Assert.fail("Domain is mandatory when project is specified.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testLogin() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.NONE);
        client.login();
    }

    @Test
    public void testLogin_Maya() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 404)
                .responseBody("Not found");
        handler.addRequest("GET", "/qcbin/authentication-point/authenticate", 401)
                .responseHeader("WWW-Authenticate", "basic realm=\"alm realm\"")
                .responseBody("Unauthorized");
        handler.addRequest("GET", "/qcbin/authentication-point/authenticate", 200)
                .expectHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.NONE);
        client.login();
    }

    @Test
    public void testLogin_ignoreNTLM() {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 404)
                .responseBody("Not found");
        handler.addRequest("GET", "/qcbin/authentication-point/authenticate", 401)
                .responseHeader("WWW-Authenticate", "basic realm=\"alm realm\"")
                .responseHeader("WWW-Authenticate", "NTLM")
                .responseBody("Unauthorized");
        handler.addRequest("GET", "/qcbin/authentication-point/authenticate", 200)
                .expectHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.NONE);
        client.login();
    }

    @Test
    public void testSessionStrategy_AUTO_LOGIN() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        client.getForStream("/test");
    }

    @Test
    public void testSessionStrategy_AUTO_LOGIN_timeout() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 403)
                .responseBody("Session expired.");
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        client.login();
        client.getForStream("/test");
    }

    @Test
    public void testSessionStrategy_NONE() throws Exception {
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.NONE);
        client.getForStream("/test");
    }

    @Test
    public void testSetHttpProxy() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create("http://foo/qcbin", "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        client.setHttpProxy("localhost", ((ServerConnector)server.getConnectors()[0]).getLocalPort());
        client.getForStream("/test");
    }

    @Test
    public void testSetHttpProxyCredentials() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 407)
                .responseHeader("Proxy-Authenticate", "Basic realm=\"proxy realm\"")
                .responseBody("Proxy Authentication Required");
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectHeader("Proxy-Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .expectBody("<alm-authentication><user>qc_user</user><password>qc_password</password></alm-authentication>");
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create("http://foo/qcbin", "domain", "project", "qc_user", "qc_password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        client.setHttpProxy("localhost", ((ServerConnector)server.getConnectors()[0]).getLocalPort());
        client.setHttpProxyCredentials("username", "password");
        client.getForStream("/test");
    }
}
