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

import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.TestOnly;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class TroubleShootService implements RestServiceLogger {

    private PrintStream fos;
    private long requestId;
    private long lastNotification;
    private long notificationDelay = 60000;
    final Notification serviceNotification;

    public TroubleShootService() {
        serviceNotification = new Notification("HP ALM Integration", "Troubleshooting mode is on and all REST " +
                "communication is being tracked.",
                "<p>Don't forget to <a href=\"\">stop tracking</a> when no longer needed.</p>", NotificationType.INFORMATION,
                new NotificationListener() {
                    public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                        notification.expire();
                        stop();
                    }
                });
    }

    public synchronized boolean isRunning() {
        return fos != null;
    }

    public synchronized void stop() {
        if(fos != null) {
            fos.flush();
            fos = null;
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    serviceNotification.expire();
                }
            });
        }
    }

    public synchronized void start(File file) {
        if(fos != null) {
            throw new IllegalStateException("Troubleshooting already running");
        }
        if(file == null) {
            throw new IllegalArgumentException("file == null");
        }
        try {
            fos = new PrintStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized long request(final Project project, String methodName, MyInputData input, String template, Object... params) {
        long id = ++requestId;
        if(fos != null) {
            fos.println(">>>>> request: #" + id + " at " + new Date());
            fos.println(">>>>> " + methodName + " " + template + " " + Arrays.asList(params));
            if(project != null) {
                fos.println(">>>>> project: " + project.getName());
            }
            if(input != null) {
                Map<String, String> headers = input.getHeaders();
                if(headers != null) {
                    fos.println(">>>>> headers: "+ headers);
                }
                String requestData = input.getRequestData();
                if(requestData!= null) {
                    fos.println(">>>>> data: "+ requestData);
                }
            }
            fos.flush();

            long now = System.currentTimeMillis();
            if(id % 10 == 0 && now >= lastNotification + notificationDelay && project != null) {
                lastNotification = now;
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        serviceNotification.expire();
                        Notifications.Bus.notify(serviceNotification, project);
                    }
                });
            }
        }
        return id;
    }

    public synchronized void response(long id, int status, MyResultInfo result) {
        if(fos != null) {
            fos.println("<<<<< response: #" + id + " at " + new Date());
            fos.println("<<<<< status: " +status);
            fos.println("<<<<< headers: "+ result.getHeaders());
            fos.println("<<<<< data: "+ result.getBodyAsString());
            fos.flush();
        }
    }

    public synchronized void loginFailure(long id, AuthenticationFailureException e) {
        if(fos != null) {
            fos.println("<<<<< login failure: #" + id + " at " + new Date());
            fos.println("<<<<< "+e.getMessage());
            fos.flush();
        }
    }

    @TestOnly
    void _setNotificationDelay(long delay) {
        this.notificationDelay = delay;
    }

    @TestOnly
    void _reset() {
        requestId = 0;
    }
}
