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

import com.hp.alm.ali.rest.client.exception.HttpClientErrorException;
import com.hp.alm.ali.rest.client.exception.HttpServerErrorException;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public static void authenticate() {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
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
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/test", 200);

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        client.getForStream("/test");
    }

    @Test
    public void testSessionStrategy_AUTO_LOGIN_timeout() throws Exception {
        authenticate();
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

    @Test
    public void testNTLMEnabledProxy() throws Exception {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 407)
                .responseHeader("Proxy-Authenticate", "Basic realm=\"proxy realm\"")
                .responseHeader("Proxy-Authenticate", "NTLM")
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

    @Test
    public void testGetForString() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 200)
                .responseBody("result");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        String result = client.getForString("/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals("result", result);
    }

    @Test
    public void testGetForString_error() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 400)
                .reasonPhrase("bad request");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.getForString("/path/{0}/{1}", "arg1", "arg2");
            Assert.fail("HttpClientErrorException expected");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(400, e.getHttpStatus());
            Assert.assertEquals(getServerUrl("/qcbin/rest/domains/domain/projects/project/path/arg1/arg2"), e.getLocation());
            Assert.assertEquals("bad request", e.getReasonPhrase());
        }
    }

    @Test
    public void testGetForStream() throws IOException {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 200)
                .responseBody("result");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        InputStream result = client.getForStream("/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals("result", IOUtils.toString(result));
    }

    @Test
    public void testGetForStream_error() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 500)
                .reasonPhrase("server failure");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.getForStream("/path/{0}/{1}", "arg1", "arg2");
            Assert.fail("HttpServerErrorException expected");
        } catch (HttpServerErrorException e) {
            Assert.assertEquals(500, e.getHttpStatus());
            Assert.assertEquals(getServerUrl("/qcbin/rest/domains/domain/projects/project/path/arg1/arg2"), e.getLocation());
            Assert.assertEquals("server failure", e.getReasonPhrase());
        }
    }

    @Test
    public void testGet() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 200)
                .responseHeader("custom", "value")
                .responseBody("result");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        ResultInfo resultInfo = ResultInfo.create(true, new ByteArrayOutputStream());
        int code = client.get(resultInfo, "/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals(200, code);
        Assert.assertEquals("result", resultInfo.getBodyStream().toString());
        Assert.assertEquals("value", resultInfo.getHeaders().get("custom"));
    }

    @Test
    public void testGet_loginError() {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 500)
                .reasonPhrase("fatal error");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        ResultInfo resultInfo = ResultInfo.create(true, new ByteArrayOutputStream());
        int code = client.get(resultInfo, "/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals(500, code);
        Assert.assertEquals("fatal error", resultInfo.getReasonPhrase());
        Assert.assertEquals(getServerUrl("/qcbin/authentication-point/alm-authenticate [on-behalf-of: " +
                getServerUrl("/qcbin/rest/domains/domain/projects/project/path/arg1/arg2") + "]"), resultInfo.getLocation());
    }

    @Test
    public void testGetForStream_loginError() {
        handler.addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 400)
                .reasonPhrase("bad request");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.getForStream("/path/{0}/{1}", "arg1", "arg2");
            Assert.fail("HttpClientErrorException expected");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(400, e.getHttpStatus());
            Assert.assertEquals(getServerUrl("/qcbin/authentication-point/alm-authenticate [on-behalf-of: " +
                    getServerUrl("/qcbin/rest/domains/domain/projects/project/path/arg1/arg2") + "]"), e.getLocation());
            Assert.assertEquals("bad request", e.getReasonPhrase());
        }
    }

    @Test
    public void testPost() {
        authenticate();
        handler.addRequest("POST", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 200)
                .expectHeader("header-input", "value-input")
                .expectBody("input")
                .responseHeader("header-output", "value-output")
                .responseBody("output");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        ResultInfo resultInfo = ResultInfo.create(true, new ByteArrayOutputStream());
        int code = client.post(InputData.create("input", Collections.singletonMap("header-input", "value-input")), resultInfo, "/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals(200, code);
        Assert.assertEquals("output", resultInfo.getBodyStream().toString());
        Assert.assertEquals("value-output", resultInfo.getHeaders().get("header-output"));
    }

    @Test
    public void testPut() {
        authenticate();
        handler.addRequest("PUT", "/qcbin/rest/domains/domain/projects/project/path/arg1/arg2", 200)
                .expectHeader("header-input", "value-input")
                .expectBody("input")
                .responseHeader("header-output", "value-output")
                .responseBody("output");

        AliRestClient client = AliRestClient.create(getQcUrl(), "domain", "project", "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        ResultInfo resultInfo = ResultInfo.create(true, new ByteArrayOutputStream());
        int code = client.put(InputData.create("input", Collections.singletonMap("header-input", "value-input")), resultInfo, "/path/{0}/{1}", "arg1", "arg2");
        Assert.assertEquals(200, code);
        Assert.assertEquals("output", resultInfo.getBodyStream().toString());
        Assert.assertEquals("value-output", resultInfo.getHeaders().get("header-output"));
    }

    @Test
    public void testListDomains() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains", 200)
                .responseBody("<Domains>" +
                        " <Domain Name='emea'/>" +
                        " <Domain Name='asia'/>" +
                        " <Domain Name='pacific'/>" +
                        "</Domains>");

        AliRestClient client = AliRestClient.create(getQcUrl(), null, null, "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        List<String> domains = client.listDomains();
        Assert.assertEquals(Arrays.asList("emea", "asia", "pacific"), domains);
    }

    @Test
    public void testListCurrentProjects() {
        authenticate();
        handler.addRequest("GET", "/qcbin/rest/domains/emea/projects", 200)
                .responseBody("<Projects>" +
                        " <Project Name='first'/>" +
                        " <Project Name='second'/>" +
                        " <Project Name='third'/>" +
                        "</Projects>");

        AliRestClient client = AliRestClient.create(getQcUrl(), "emea", null, "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        List<String> projects = client.listCurrentProjects();
        Assert.assertEquals(Arrays.asList("first", "second", "third"), projects);
    }

    @Test
    public void testListCurrentProjects_noDomain() {
        AliRestClient client = AliRestClient.create(getQcUrl(), null, null, "user", "password", AliRestClient.SessionStrategy.AUTO_LOGIN);
        try {
            client.listCurrentProjects();
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // domain not selected
        }
    }
}
