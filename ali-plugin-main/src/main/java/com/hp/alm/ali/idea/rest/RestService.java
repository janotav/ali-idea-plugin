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

import com.hp.alm.ali.idea.cfg.AuthenticationFailed;
import com.hp.alm.ali.idea.services.WeakListeners;
import com.hp.alm.ali.idea.cfg.AliConfigurable;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.ConfigurationListener;
import com.hp.alm.ali.idea.model.ServerStrategy;
import com.hp.alm.ali.rest.client.AliRestClient;
import com.hp.alm.ali.rest.client.ResultInfo;
import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

import javax.swing.event.HyperlinkEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RestService implements ConfigurationListener {

    private ServerType serverType = ServerType.NONE;
    volatile private AliRestClient restClient;
    private WeakListeners<RestListener> listeners;
    private WeakListeners<ServerTypeListener> serverTypeListeners;
    private Project project;
    private AliProjectConfiguration projConf;
    private TroubleShootService troubleShootService;
    final Notification errorNotification;

    public RestService(final Project project, TroubleShootService troubleShootService, AliProjectConfiguration conf) {
        this.project = project;
        this.troubleShootService = troubleShootService;
        this.projConf = conf;
        listeners = new WeakListeners<RestListener>();
        serverTypeListeners = new WeakListeners<ServerTypeListener>();
        conf.addListener(this);
        ApplicationManager.getApplication().getComponent(AliConfiguration.class).addListener(this);

        errorNotification = new Notification("HP ALM Integration", "Cannot connect to HP ALM",
                "<p><a href=\"\">Configure HP ALM integration ...</a></p>", NotificationType.ERROR,
                new NotificationListener() {
                    public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                        notification.expire();
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, AliConfigurable.NAME);
                    }
                });
    }

    public void launchProjectUrl(String href) {
        String query =  href.contains("?")? "&": "?";
        BrowserUtil.launchBrowser(projConf.getLocation() + "/rest/domains/" + projConf.getDomain() + "/projects/" + projConf.getProject() + "/" + href + query + "login-form-required=Y");
    }

    private AliRestClient createRestClient(AliProjectConfiguration conf) {
        return createRestClient(conf.getLocation(), conf.getDomain(), conf.getProject(), conf.getUsername(), conf.getPassword(), AliRestClient.SessionStrategy.AUTO_LOGIN);
    }

    public static AliRestClient createRestClient(String location, String domain, String project, String username, String password, AliRestClient.SessionStrategy strategy) {
        AliRestClient restClient = AliRestClient.create(location, domain, project, username, password, strategy);
        restClient.setEncoding(null);
        restClient.setTimeout(10000);
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        if(httpConfigurable.USE_HTTP_PROXY) {
            restClient.setHttpProxy(httpConfigurable.PROXY_HOST, httpConfigurable.PROXY_PORT);
            if(httpConfigurable.PROXY_AUTHENTICATION) {
                String passwd = httpConfigurable.getPlainProxyPassword();
                if(passwd.isEmpty()) {
                    httpConfigurable.getPromptedAuthentication("HP ALI", "Enter HTTP Proxy Credentials:");
                    passwd = httpConfigurable.getPlainProxyPassword();
                }
                restClient.setHttpProxyCredentials(httpConfigurable.PROXY_LOGIN, passwd);
            }
        }
        return restClient;
    }

    public synchronized AliRestClient getRestClient() {
        if(restClient == null) {
            restClient = createRestClient(projConf);
        }
        return restClient;
    }

    public int get(MyResultInfo result, String template, Object... params) {
        return execute(getRestClient(), project, troubleShootService, new MyGetMethod(), null, result, template, params);
    }

    public int put(String xml, MyResultInfo result, String template, Object... params) {
        return put(new MyInputData(xml), result, template, params);
    }

    public int put(MyInputData inputData, MyResultInfo result, String template, Object... params) {
        return execute(getRestClient(), project, troubleShootService, new MyPutMethod(), inputData, result, template, params);
    }

    public int post(MyInputData inputData, MyResultInfo result, String template, Object... params) {
        return execute(getRestClient(), project, troubleShootService, new MyPostMethod(), inputData, result, template, params);
    }

    public int post(String xml, MyResultInfo result, String template, Object... params) {
        return post(new MyInputData(xml), result, template, params);
    }

    public int delete(MyResultInfo result, String template, Object... params) {
        return execute(getRestClient(), project, troubleShootService, new MyDeleteMethod(), null, result, template, params);
    }

    public void delete(String template, Object... params) {
        execute(getRestClient(), project, troubleShootService, new MyDeleteMethod(), null, new MyResultInfo(), template, params);
    }

    public static String getForString(AliRestClient restClient, String template, Object ... params) {
        TroubleShootService troubleShootService = ApplicationManager.getApplication().getComponent(TroubleShootService.class);
        MyResultInfo result = new MyResultInfo();
        int status = execute(restClient, null, troubleShootService, new MyGetMethod(), null, result, template, params);
        if(status < 200 || status > 299) {
            throw new RestException(result);
        }
        return result.getBodyAsString();
    }

    public InputStream getForStream(String template, Object ... params) {
        MyResultInfo result = new MyResultInfo();
        int status = get(result, template, params);
        if(status < 200 || status > 299) {
            throw new RestException(result);
        }
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                errorNotification.expire();
            }
        });
        return result.getBodyAsStream();
    }

    public int getThatCanDelegate(OutputStream output, String template, Object... params) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MyResultInfo result = new MyResultInfo();
        int code = get(result, template, params);
        ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            if(code != HttpStatus.SC_OK) {
                IOUtils.copy(is, output);
                return code;
            }
            String delegated = result.getHeaders().get("X-Bridge-URL");
            if(delegated == null) {
                IOUtils.copy(is, output);
                return code;
            } else {
                PostMethod post = new PostMethod(delegated);
                post.setRequestEntity(new InputStreamRequestEntity(is, baos.size()));
                code = new HttpClient().executeMethod(post);
                IOUtils.copy(post.getResponseBodyAsStream(), output);
                return code;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int execute(AliRestClient restClient, Project project, TroubleShootService troubleShootService, MyMethod method, MyInputData myInput, MyResultInfo myResult, String template, Object... params) {
        ResultInfo info = ResultInfo.create(true, myResult.getOutputStream());
        long id = troubleShootService.request(project, method.getName(), myInput, template, params);
        int code;
        try {
            code = method.execute(restClient, myInput == null? null: myInput.getInputData(), info, template, params);
        } catch(AuthenticationFailureException e) {
            troubleShootService.loginFailure(id, e);
            throw e;
        }
        myResult.copyFrom(info);
        troubleShootService.response(id, code, myResult);
        return code;
    }

    public void addListener(RestListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RestListener listener) {
        listeners.remove(listener);
    }

    public void onChanged() {
        try {
            synchronized (this) {
                if(restClient != null) {
                    logout(restClient);
                }
                serverType = ServerType.NONE; // fire event later (when connecting)
                restClient = createRestClient(projConf);
            }
            fireRestConfigurationChanged();
        } finally {
            checkConnectivity();
        }
    }

    public static void logout(final AliRestClient client) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                if(client.getSessionContext() != null) {
                    client.logout();
                }
            }
        });
    }

    private void fireRestConfigurationChanged() {
        listeners.fire(new WeakListeners.Action<RestListener>() {
            public void fire(RestListener listener) {
                listener.restConfigurationChanged();
            }
        });
    }

    public void checkConnectivity() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                try {
                    if(setConnectingType()) {
                        setServerType(AliConfigurable.getServerType(getRestClient(), false));
                    }
                } catch(Exception e) {
                    if(e instanceof AuthenticationFailed && projConf.getPassword().isEmpty()) {
                        setServerType(ServerType.NEEDS_PASSWORD);
                    } else {
                        setServerType(ServerType.NONE);
                    }
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        public void run() {
                            ToolWindow toolWindow = project.getComponent(ToolWindowManager.class).getToolWindow("HP ALI");
                            if (toolWindow != null && toolWindow.getContentManager().getContentCount() > 1) {
                                expireConnectivityError();
                                Notifications.Bus.notify(errorNotification, project);
                            }
                        }
                    });
                }
            }
        });
    }

    public void expireConnectivityError() {
        errorNotification.expire();
    }

    public synchronized ServerType getServerTypeIfAvailable() {
        return serverType;
    }

    public synchronized boolean serverTypeIsApollo() {
        return serverType != null && serverType.isApollo();
    }

    public synchronized ServerType getServerType() throws InterruptedException {
        while(serverType == ServerType.CONNECTING) {
            wait();
        }
        return serverType;
    }

    public synchronized ServerStrategy getModelCustomization() {
        return project.getComponent(serverType.getClazz());
    }

    private boolean setConnectingType() {
        synchronized (this) {
            if(serverType != ServerType.NONE) {
                return false;
            }
            serverType = ServerType.CONNECTING;
            notifyAll();
        }
        fireServerTypeEvent();
        return true;
    }

    private void setServerType(ServerType serverType) {
        synchronized (this) {
            this.serverType = serverType;
            notifyAll();
        }
        fireServerTypeEvent();
    }

    private void fireServerTypeEvent() {
        serverTypeListeners.fire(new WeakListeners.Action<ServerTypeListener>() {
            public void fire(ServerTypeListener listener) {
                listener.connectedTo(serverType);
            }
        });
    }

    public void addServerTypeListener(ServerTypeListener listener) {
        addServerTypeListener(listener, true);
    }

    public void addServerTypeListener(ServerTypeListener listener, boolean weak) {
        serverTypeListeners.add(listener, weak);
    }

    public void removeServerTypeListener(ServerTypeListener listener) {
        serverTypeListeners.remove(listener);
    }
}
