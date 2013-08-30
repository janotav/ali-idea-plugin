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
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Handler extends AbstractHandler {

    private LinkedList<MyRequest> requests;
    private Throwable firstError;

    public Handler() {
        this.requests = new LinkedList<MyRequest>();
    }

    public MyRequest addRequest(String method, String url, int responseCode) {
        MyRequest request = new MyRequest(method, url, responseCode);
        requests.add(request);
        return request;
    }

    @Override
    public void handle(String url, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MyRequest myRequest = requests.removeFirst();
        if(firstError == null) {
            if(myRequest.reasonPhrase != null) {
                response.setStatus(myRequest.responseCode, myRequest.reasonPhrase);
            } else {
                response.setStatus(myRequest.responseCode);
            }
            baseRequest.setHandled(true);
            try {
                Assert.assertEquals(myRequest.method, baseRequest.getMethod());
                Assert.assertEquals(myRequest.url, baseRequest.getUri().getPath());
                myRequest.evaluateAssertions(baseRequest, request, response);
                myRequest.evaluateActions(response);
            } catch (Throwable t) {
                firstError = t;
            }
        } else {
            // once there was an error, simply fail all requests
            response.setStatus(500);
            baseRequest.setHandled(true);
        }
    }

    public void done() {
        Assert.assertNull(firstError);
        Assert.assertTrue(requests.isEmpty());
    }

    public void clear() {
        firstError = null;
        requests.clear();
    }

    private interface Assertion {

        void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;

    }

    private interface Action {

        void evaluate(HttpServletResponse response) throws IOException;

    }

    public static class MyRequest {
        private List<Assertion> assertions;
        private List<Action> actions;
        private String method;
        private String url;
        private int responseCode;
        private String reasonPhrase;

        private MyRequest(String method, String url, int responseCode) {
            this.method = method;
            this.url = url;
            this.responseCode = responseCode;
            assertions = new LinkedList<Assertion>();
            actions = new LinkedList<Action>();
        }

        public void evaluateAssertions(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            for(Assertion assertion: assertions) {
                assertion.evaluate(baseRequest, request, response);
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
                public void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                    Assert.assertEquals(value, baseRequest.getHeader(header));
                }
            });
            return this;
        }

        public MyRequest expectBody(final String value) {
            assertions.add(new Assertion() {
                @Override
                public void evaluate(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                    Assert.assertEquals(value, IOUtils.toString(baseRequest.getInputStream()));
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
    }
}
