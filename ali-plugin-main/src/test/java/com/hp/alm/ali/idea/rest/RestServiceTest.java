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

package com.hp.alm.ali.idea.rest;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.FixtureFactory;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.model.HorizonStrategy;
import com.hp.alm.ali.idea.model.ServerStrategy;
import com.hp.alm.ali.rest.client.AliRestClientFactory;
import com.hp.alm.ali.rest.client.InputData;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.rest.client.RestClientFactory;
import com.hp.alm.ali.rest.client.ResultInfo;
import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.net.HttpConfigurable;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RestServiceTest extends IntellijTest {

    private RestService restService;
    private RestClient restClient;
    private AliProjectConfiguration projectConfiguration;

    @Before
    public void preClean() {
        restService = getComponent(RestService.class);
        restClient = restService.getRestClient();
        projectConfiguration = getComponent(AliProjectConfiguration.class);
    }

    @After
    public void postClean() throws Throwable {
        handler.checkpoint();
        RestService._setFactory(AliRestClientFactory.getInstance());
        restService._setRestServiceLogger(ApplicationManager.getApplication().getComponent(TroubleShootService.class));
        if(restService.getRestClient() instanceof MockRestClient) {
            restService._setRestClient(restClient);
        }
        HttpConfigurable.getInstance().USE_HTTP_PROXY = false;
    }

    public RestServiceTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testCreateRestClient_noProxy() {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        httpConfigurable.USE_HTTP_PROXY = false;

        handler.async(3);
        RestService._setFactory(new RestClientFactory() {
            @Override
            public RestClient create(final String location, final String domain, final String project, final String userName, final String password, final RestClient.SessionStrategy sessionStrategy) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("location", location);
                        Assert.assertEquals("domain", domain);
                        Assert.assertEquals("project", project);
                        Assert.assertEquals("user", userName);
                        Assert.assertEquals("password", password);
                        Assert.assertEquals(RestClient.SessionStrategy.AUTO_LOGIN, sessionStrategy);
                    }
                });
                return new MockRestClient() {

                    @Override
                    public void setEncoding(final String encoding) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertNull(encoding);
                            }
                        });
                    }

                    @Override
                    public void setTimeout(final int timeout) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals(10000, timeout);
                            }
                        });
                    }
                };
            }
        });

        RestService.createRestClient("location", "domain", "project", "user", "password", RestClient.SessionStrategy.AUTO_LOGIN);
    }

    @Test
    public void testCreateRestClient_unauthenticatedProxy() {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        httpConfigurable.USE_HTTP_PROXY = true;
        httpConfigurable.PROXY_HOST = "localhost";
        httpConfigurable.PROXY_PORT = 1234;
        httpConfigurable.PROXY_AUTHENTICATION = false;

        handler.async(4);
        RestService._setFactory(new RestClientFactory() {
            @Override
            public RestClient create(final String location, final String domain, final String project, final String userName, final String password, final RestClient.SessionStrategy sessionStrategy) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("http://location", location);
                        Assert.assertEquals("domain", domain);
                        Assert.assertEquals("project", project);
                        Assert.assertEquals("user", userName);
                        Assert.assertEquals("password", password);
                        Assert.assertEquals(RestClient.SessionStrategy.AUTO_LOGIN, sessionStrategy);
                    }
                });
                return new MockRestClient() {

                    @Override
                    public void setHttpProxy(final String proxyHost, final int proxyPort) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals("localhost", proxyHost);
                                Assert.assertEquals(1234, proxyPort);
                            }
                        });
                    }

                    @Override
                    public void setEncoding(final String encoding) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertNull(encoding);
                            }
                        });
                    }

                    @Override
                    public void setTimeout(final int timeout) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals(10000, timeout);
                            }
                        });
                    }
                };
            }
        });

        RestService.createRestClient("http://location", "domain", "project", "user", "password", RestClient.SessionStrategy.AUTO_LOGIN);
    }

    @Test
    public void testCreateRestClient_authenticatedProxy() {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        httpConfigurable.USE_HTTP_PROXY = true;
        httpConfigurable.PROXY_HOST = "localhost";
        httpConfigurable.PROXY_PORT = 1234;
        httpConfigurable.PROXY_AUTHENTICATION = true;
        httpConfigurable.PROXY_LOGIN = "admin";
        httpConfigurable.PROXY_PASSWORD_CRYPT = "Y2hhbmdlaXQ=";

        handler.async(5);
        RestService._setFactory(new RestClientFactory() {
            @Override
            public RestClient create(final String location, final String domain, final String project, final String userName, final String password, final RestClient.SessionStrategy sessionStrategy) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("http://location", location);
                        Assert.assertEquals("domain", domain);
                        Assert.assertEquals("project", project);
                        Assert.assertEquals("user", userName);
                        Assert.assertEquals("password", password);
                        Assert.assertEquals(RestClient.SessionStrategy.AUTO_LOGIN, sessionStrategy);
                    }
                });
                return new MockRestClient() {

                    @Override
                    public void setHttpProxy(final String proxyHost, final int proxyPort) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals("localhost", proxyHost);
                                Assert.assertEquals(1234, proxyPort);
                            }
                        });
                    }

                    @Override
                    public void setHttpProxyCredentials(final String username, final String password) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals("admin", username);
                                Assert.assertEquals("changeit", password);
                            }
                        });
                    }

                    @Override
                    public void setEncoding(final String encoding) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertNull(encoding);
                            }
                        });
                    }

                    @Override
                    public void setTimeout(final int timeout) {
                        handler.done(new Runnable() {
                            @Override
                            public void run() {
                                Assert.assertEquals(10000, timeout);
                            }
                        });
                    }
                };
            }
        });

        RestService.createRestClient("http://location", "domain", "project", "user", "password", RestClient.SessionStrategy.AUTO_LOGIN);
    }

    @Test
    public void testGetRestClient() {
        RestClient restClient = restService.getRestClient();
        Assert.assertNotNull(restClient);
        Assert.assertEquals(RestClient.SessionStrategy.AUTO_LOGIN, restClient.getSessionStrategy());
        Assert.assertEquals(restClient, restService.getRestClient());
    }

    @Test
    public void testGet() {
        handler.async();
        restService._setRestClient(new MockRestClient() {

            @Override
            public int get(ResultInfo result, final String template, final Object... params) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defects/{0}", template);
                        Assert.assertEquals("1", params[0]);
                    }
                });
                try {
                    result.getBodyStream().write("foo".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return 200;
            }
        });
        MyResultInfo result = new MyResultInfo();
        int status = restService.get(result, "defects/{0}", "1");
        Assert.assertEquals(200, status);
        Assert.assertEquals("foo", result.getBodyAsString());
    }

    @Test
    public void testPut() {
        handler.async();
        restService._setRestClient(new MockRestClient() {

            @Override
            public int put(final InputData inputData, ResultInfo result, final String template, final Object... params) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defects/{0}", template);
                        Assert.assertEquals("1", params[0]);
                        Assert.assertEquals("bar", asString(inputData.getRequestEntity(null)));
                    }
                });
                try {
                    result.getBodyStream().write("foo".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return 200;
            }
        });
        MyResultInfo result = new MyResultInfo();
        int status = restService.put("bar", result, "defects/{0}", "1");
        Assert.assertEquals(200, status);
        Assert.assertEquals("foo", result.getBodyAsString());
    }

    @Test
    public void testPost() {
        handler.async();
        restService._setRestClient(new MockRestClient() {

            @Override
            public int post(final InputData inputData, ResultInfo result, final String template, final Object... params) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defects/{0}", template);
                        Assert.assertEquals("1", params[0]);
                        Assert.assertEquals("bar", asString(inputData.getRequestEntity(null)));
                    }
                });
                try {
                    result.getBodyStream().write("foo".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return 200;
            }
        });
        MyResultInfo result = new MyResultInfo();
        int status = restService.post("bar", result, "defects/{0}", "1");
        Assert.assertEquals(200, status);
        Assert.assertEquals("foo", result.getBodyAsString());
    }

    @Test
    public void testDelete() {
        handler.async();
        restService._setRestClient(new MockRestClient() {

            @Override
            public int delete(ResultInfo result, final String template, final Object... params) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("defects/{0}", template);
                        Assert.assertEquals("1", params[0]);
                    }
                });
                try {
                    result.getBodyStream().write("foo".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return 200;
            }
        });
        MyResultInfo result = new MyResultInfo();
        int status = restService.delete(result, "defects/{0}", "1");
        Assert.assertEquals(200, status);
        Assert.assertEquals("foo", result.getBodyAsString());
    }

    @Test
    public void testRestServiceLogger() {
        handler.async(2);
        restService._setRestServiceLogger(new MockRestServiceLogger() {

            @Override
            public long request(Project project, final String name, final MyInputData myInput, final String template, Object... params) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("POST", name);
                        Assert.assertEquals("defects", template);
                        Assert.assertEquals("bar", myInput.getRequestData());
                    }
                });
                return 1;
            }

            @Override
            public void response(final long id, final int code, final MyResultInfo myResult) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(1, id);
                        Assert.assertEquals(200, code);
                        Assert.assertEquals("foo", myResult.getBodyAsString());
                    }
                });
            }
        });
        restService._setRestClient(new MockRestClient() {

            @Override
            public int post(InputData input, ResultInfo result, final String template, final Object... params) {
                try {
                    result.getBodyStream().write("foo".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return 200;
            }
        });
        restService.post("bar", new MyResultInfo(), "defects");
    }

    @Test
    public void testRestServiceLogger_loginFailure() {
        handler.async();
        restService._setRestServiceLogger(new MockRestServiceLogger() {

            @Override
            public long request(Project project, String name, MyInputData myInput, String template, Object... params) {
                return 1;
            }

            @Override
            public void loginFailure(final long id, final AuthenticationFailureException e) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals(1, id);
                        Assert.assertEquals(401, e.getHttpStatus());
                        Assert.assertEquals("Forbidden", e.getReasonPhrase());
                        Assert.assertEquals("defects", e.getLocation());
                    }
                });
            }
        });
        restService._setRestClient(new MockRestClient() {

            @Override
            public int get(ResultInfo result, final String template, final Object... params) {
                result.setHttpStatus(401);
                result.setReasonPhrase("Forbidden");
                result.setLocation(template);
                throw new AuthenticationFailureException(result);
            }
        });
        try {
            restService.get(new MyResultInfo(), "defects");
            Assert.fail("should have failed");
        } catch (AuthenticationFailureException e) {
            // expected
        }
    }

    @Test
    public void testOnChangedEvent() throws InterruptedException {
        // when configuration is changed, following should happen:
        //
        //  1. logout request is performed
        //  2. listeners receive REST configuration changed event
        //  3. listeners receive server type CONNECTING event
        //  4. server version is negotiated
        //  5. listeners receive server type AGM event

        handler.addRequest(false, "GET", "/qcbin/authentication-point/logout", 200);
        FixtureFactory.handshake(handler, true);

        handler.async(3);
        restService.addListener(new OneTimeRestListener(handler, restService) {
            @Override
            protected void restConfigurationChangedEvent() {
                // async done (don't remove)
            }
        });

        restService.addServerTypeListener(new OneTimeServerTypeListener(handler, restService) {
            @Override
            protected void connectedToEvent(ServerType serverType) {
                Assert.assertEquals(ServerType.CONNECTING, serverType);
                restService.addServerTypeListener(new OneTimeServerTypeListener(handler, restService) {
                    @Override
                    protected void connectedToEvent(ServerType serverType) {
                        Assert.assertEquals(ServerType.AGM, serverType);
                    }
                });
            }
        });

        projectConfiguration.fireChanged();

        // wait for handshake finish
        handler.consume();
    }

    @Test
    public void testLogout() {
        handler.async();
        RestService.logout(new MockRestClient() {
            @Override
            public void logout() {
                handler.done();
            }
        });
    }

    @Test
    public void testCheckConnectivity_whenDisconnected() {
        // in the disconnected state perform checkConnectivity:
        //
        //  1. listeners receive server type CONNECTING event
        //  2. server version is negotiated
        //  3. listeners receive server type AGM event

        FixtureFactory.handshake(handler, false);

        handler.async(2);
        restService.addServerTypeListener(new OneTimeServerTypeListener(handler, restService) {
            @Override
            protected void connectedToEvent(ServerType serverType) {
                Assert.assertEquals(ServerType.CONNECTING, serverType);

                restService.addServerTypeListener(new OneTimeServerTypeListener(handler, restService) {
                    @Override
                    protected void connectedToEvent(ServerType serverType) {
                        Assert.assertEquals(ServerType.AGM, serverType);
                    }
                });
            }
        });

        restService._setServerType(ServerType.NONE);
        restService.checkConnectivity();

        handler.consume();
    }

    @Test
    public void testCheckConnectivity_whenConnected() {
        // nothing should happen when connected
        restService.checkConnectivity();
    }

    @Test
    public void testGetServerType() throws InterruptedException {
        try {
            Assert.assertEquals(ServerType.AGM, restService.getServerType());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        restService._setServerType(ServerType.NONE);

        try {
            Assert.assertEquals(ServerType.NONE, restService.getServerType());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        restService._setServerType(ServerType.CONNECTING);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                restService._setServerType(ServerType.AGM);
            }
        });
        Assert.assertEquals(ServerType.AGM, restService.getServerType());
    }

    @Test
    public void testGetServerType_whenConnected() throws InterruptedException {
        Assert.assertEquals(ServerType.AGM, restService.getServerType());
    }

    @Test
    public void testGetServerStrategy() throws InterruptedException {
        restService._setServerType(ServerType.NONE);

        try {
            restService.getServerStrategy();
            Assert.fail("Should have failed");
        } catch (NotConnectedException e) {
            // ok
        }

        restService._setServerType(ServerType.CONNECTING);

        try {
            restService.getServerStrategy();
            Assert.fail("Should have failed");
        } catch (NotConnectedException e) {
            // ok
        }

        restService._setServerType(ServerType.AGM);

        ServerStrategy serverStrategy = restService.getServerStrategy();
        Assert.assertEquals(HorizonStrategy.class, serverStrategy.getClass());
    }

    private String asString(RequestEntity requestEntity) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            requestEntity.writeRequest(baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toString();
    }
}
