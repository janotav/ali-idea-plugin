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

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Handler extends AbstractHandler {

    final private LinkedList<MyRequest> requests;
    private LinkedList<Runnable> cleanup;
    private Throwable firstError;
    private Semaphore async = new Semaphore(100);
    private ServerVersion version;

    public Handler(ServerVersion version) throws Exception {
        this.version = version;
        this.requests = new LinkedList<MyRequest>();
        this.cleanup = new LinkedList<Runnable>();

        new Server(0).setHandler(this);
    }

    public void async() {
        async(1);
    }

    public void async(int count) {
        async.acquireUninterruptibly(count);
    }

    public void done() {
        done(1);
    }

    public void done(int count) {
        async.release(count);
    }

    public void fail(String message) {
        if(firstError == null) {
            try {
                Assert.fail(message);
            } catch (Throwable t) {
                firstError = t;
            }
        }
        done();
    }

    public void done(Runnable runnable) {
        if(firstError == null) {
            try {
                runnable.run();
            } catch (Throwable t) {
                firstError = t;
            }
        }
        done();
    }

    public MyRequest addRequest(String method, String url, int responseCode) {
        return addRequest(true, method, url, responseCode);
    }

    public MyRequest addRequest(boolean strict, String method, String url, int responseCode) {
        MyRequest request = new MyRequest(strict, method, url, responseCode);
        synchronized (requests) {
            requests.add(request);
        }
        return request;
    }

    public int getLocalPort() {
        return ((ServerConnector)getServer().getConnectors()[0]).getLocalPort();
    }

    public String getQcUrl() {
        return getServerUrl("/qcbin");
    }

    public String getServerUrl(String path) {
        return "http://localhost:" + getLocalPort() + path;
    }

    @Override
    public void handle(String url, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String urlWithQuery = baseRequest.getUri().getPath();
        if(baseRequest.getUri().hasQuery()) {
            urlWithQuery += "?" + URLDecoder.decode(baseRequest.getQueryString(), "UTF-8");
        }

        System.out.println("[incoming] " + baseRequest.getMethod() + " " + urlWithQuery);
        baseRequest.setHandled(true);
        if(firstError != null) {
            System.out.println("[ignored]");
            // once there was an error, simply fail all requests
            response.setStatus(500);
            return;
        }
        MyRequest myRequest = null;
        try {
            synchronized (requests) {
                for(MyRequest candidate: requests) {
                    if(candidate.strict) {
                        break;
                    }
                    try {
                        Assert.assertEquals(candidate.method, baseRequest.getMethod());
                        Assert.assertEquals(candidate.url, urlWithQuery);
                        candidate.evaluateAssertions(baseRequest, request, response, true);
                        requests.remove(candidate);
                        myRequest = candidate;
                        break;
                    } catch (Throwable t) {
                        // negative
                    }
                }
                if(myRequest == null) {
                    if(requests.isEmpty()) {
                        throw new Exception("Unexpected request: " + baseRequest.getMethod() + " " + urlWithQuery);
                    }
                    myRequest = requests.removeFirst();
                }
                requests.notifyAll();
            }
            Assert.assertEquals(myRequest.method, baseRequest.getMethod());
            Assert.assertEquals(myRequest.url, urlWithQuery);
            myRequest.evaluateAssertions(baseRequest, request, response, false);
            myRequest.evaluateActions(response);
            if(myRequest.reasonPhrase != null) {
                response.setStatus(myRequest.responseCode, myRequest.reasonPhrase);
            } else {
                response.setStatus(myRequest.responseCode);
            }
            System.out.println("[success]");
        } catch (Throwable t) {
            System.out.println("[failed] "+t.getMessage());
            firstError = t;
            response.setStatus(500);
            async.release(100);
        }
    }

    public void authenticate() {
        addRequest("POST", "/qcbin/authentication-point/alm-authenticate", 200)
                .expectXmlBody("<alm-authentication><user>user</user><password>password</password></alm-authentication>");
        addRequest("POST", "/qcbin/rest/site-session", 200)
                .expectXmlBody("<session-parameters><client-type>ALI_IDEA_plugin</client-type></session-parameters>");
    }

    public void checkpoint() throws Throwable {
        Assert.assertTrue("Test timed out", async.tryAcquire(100, 2000, TimeUnit.MILLISECONDS));
        async.release(100);
    }

    public void finish() throws Throwable {
        Assert.assertTrue("Test timed out", async.tryAcquire(100, 2000, TimeUnit.MILLISECONDS));
        if(firstError != null) {
            throw firstError;
        }
        Assert.assertTrue("There are unresponded requests", requests.isEmpty());
        for(Runnable action: cleanup) {
            action.run();
        }
    }

    public void clear() {
        async = new Semaphore(100);
        firstError = null;
        requests.clear();
        cleanup.clear();
    }

    public String getContent(String content) throws IOException {
        InputStream is = Handler.class.getResourceAsStream("/" + version.name() + "/" + content);
        if(is == null) {
            is = Handler.class.getResourceAsStream("/" + content);
        }
        if(is == null) {
            throw new IOException("Content '"+content+"' not available for '" + version + "'.");
        } else {
            return IOUtils.toString(is);
        }
    }

    public void consume() throws RuntimeException {
        if(firstError != null) {
            throw new RuntimeException(firstError);
        }

        synchronized (requests) {
            while (!requests.isEmpty()) {
                try {
                    requests.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public ServerVersion getVersion() {
        return version;
    }

    public void addCleanup(Runnable action) {
        cleanup.add(action);
    }

    private interface Assertion {

        void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response, boolean soft) throws IOException;

    }

    private interface Action {

        void evaluate(HttpServletResponse response) throws IOException;

    }

    public class MyRequest {
        private List<Assertion> assertions;
        private List<Action> actions;
        private boolean strict;
        private String method;
        private String url;
        private int responseCode;
        private String reasonPhrase;

        private MyRequest(boolean strict, String method, String url, int responseCode) {
            this.strict = strict;
            this.method = method;
            this.url = url;
            this.responseCode = responseCode;
            assertions = new LinkedList<Assertion>();
            actions = new LinkedList<Action>();
        }

        public void evaluateAssertions(Request baseRequest, HttpServletRequest request, HttpServletResponse response, boolean soft) throws IOException {
            for(Assertion assertion: assertions) {
                assertion.evaluate(baseRequest, request, response, soft);
            }
        }

        public void evaluateActions(HttpServletResponse response) throws IOException {
            for(Action action: actions) {
                action.evaluate(response);
            }
        }

        public MyRequest expectHeader(final String header, final String value) {
            assertions.add(new Assertion() {
                @Override
                public void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response, boolean soft) {
                    Assert.assertEquals(value, baseRequest.getHeader(header));
                }
            });
            return this;
        }

        public MyRequest expectXmlBody(final String value) {
            assertions.add(new Assertion() {
                @Override
                public void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response, boolean soft) throws IOException {
                    if(!soft) {
                        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + value.replaceAll("\r\n", "\n") + "\n", IOUtils.toString(baseRequest.getInputStream()).replaceAll("\r\n", "\n"));
                    }
                }
            });
            return this;
        }

        public MyRequest expectBody(final String value) {
            assertions.add(new Assertion() {
                @Override
                public void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response, boolean soft) throws IOException {
                    if(!soft) {
                        Assert.assertEquals(value, IOUtils.toString(baseRequest.getInputStream()));
                    }
                }
            });
            return this;
        }

        public MyRequest responseHeader(final String header, final String value) {
            actions.add(new Action() {
                @Override
                public void evaluate(HttpServletResponse response) {
                    response.addHeader(header, value);
                }
            });
            return this;
        }

        public MyRequest responseBody(final String value) {
            actions.add(new Action() {
                @Override
                public void evaluate(HttpServletResponse response) throws IOException {
                    response.getWriter().write(value);
                }
            });
            return this;
        }

        public MyRequest reasonPhrase(String reasonPhrase) {
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        public MyRequest content(final String content) {
            actions.add(new Action() {
                @Override
                public void evaluate(HttpServletResponse response) throws IOException {
                    response.getWriter().write(getContent(content));
                }
            });
            return this;
        }
    }
}
