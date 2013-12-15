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
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.rest.client.ResultInfo;
import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TroubleShootServiceTest extends IntellijTest {

    private TroubleShootService troubleShootService;

    @Before
    public void preClean() {
        troubleShootService = ApplicationManager.getApplication().getComponent(TroubleShootService.class);
        troubleShootService._reset();
    }

    public TroubleShootServiceTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testRecording() throws IOException {
        File file = File.createTempFile("trouble", "");

        ResultInfo resultInfo = ResultInfo.create(new ByteArrayOutputStream());
        resultInfo.setLocation("location");
        resultInfo.setHttpStatus(401);

        resultInfo.setReasonPhrase("failed1");
        troubleShootService.loginFailure(1, new AuthenticationFailureException(resultInfo));
        troubleShootService.request(getProject(), "GET", new MyInputData("<foo1/>"), "defects/{0}", "1");
        MyResultInfo result = new MyResultInfo();
        result.getOutputStream().write("foobar1".getBytes());
        troubleShootService.response(1, 200, result);

        Assert.assertFalse(troubleShootService.isRunning());
        troubleShootService.start(file);
        Assert.assertTrue(troubleShootService.isRunning());

        resultInfo.setReasonPhrase("failed2");
        troubleShootService.loginFailure(1, new AuthenticationFailureException(resultInfo));
        troubleShootService.request(getProject(), "GET", new MyInputData("<foo2/>"), "defects/{0}", "2");
        result = new MyResultInfo();
        result.getHeaders().put("a", "b");
        result.getOutputStream().write("foobar2".getBytes());
        troubleShootService.response(1, 200, result);

        troubleShootService.stop();
        Assert.assertFalse(troubleShootService.isRunning());

        resultInfo.setReasonPhrase("failed3");
        troubleShootService.loginFailure(1, new AuthenticationFailureException(resultInfo));
        troubleShootService.request(getProject(), "GET", new MyInputData("<foo3/>"), "defects/{0}", "3");
        result = new MyResultInfo();
        result.getOutputStream().write("foobar3".getBytes());
        troubleShootService.response(1, 200, result);

        String data = IOUtils.toString(new FileInputStream(file));

        // not started
        Assert.assertFalse(data.contains("failed1"));
        Assert.assertFalse(data.contains(">>>>> GET defects/{0} [1]"));
        Assert.assertFalse(data.contains("foobar1"));

        // failure
        Assert.assertTrue(data.contains("<<<<< login failure: #1"));
        Assert.assertTrue(data.contains("<<<<< 401 failed2 [location: location]"));

        // request
        Assert.assertTrue(data.contains(">>>>> GET defects/{0} [2]"));

        // response
        Assert.assertTrue(data.contains(">>>>> data: <foo2/>"));
        Assert.assertTrue(data.contains("<<<<< status: 200"));
        Assert.assertTrue(data.contains("<<<<< headers: {a=b}"));
        Assert.assertTrue(data.contains("<<<<< data: foobar2"));

        // already stopped
        Assert.assertFalse(data.contains("failed3"));
        Assert.assertFalse(data.contains(">>>>> GET defects/{0} [3]"));
        Assert.assertFalse(data.contains("foobar3"));
    }

    @Test
    public void testNotification() throws IOException {
        File file = File.createTempFile("trouble", "");
        troubleShootService.start(file);

        final MessageBusConnection connection = getProject().getMessageBus().connect();
        final MutableInt times = new MutableInt(0);

        connection.subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(@NotNull Notification notification) {
                Assert.assertEquals("HP ALM Integration", notification.getGroupId());
                Assert.assertEquals("Troubleshooting mode is on and all REST communication is being tracked.", notification.getTitle());
                Assert.assertEquals(NotificationType.INFORMATION, notification.getType());
                times.add(1);
            }

            @Override
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType) {
            }

            @Override
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType, boolean shouldLog) {
            }

            // needed for 13, adapter class not defined in 12.1.1
            public void register(@NotNull String groupDisplayName, @NotNull NotificationDisplayType defaultDisplayType, boolean shouldLog, boolean shouldReadAloud) {
            }
        });

        troubleShootService._setNotificationDelay(60000);
        for(int i = 0; i < 100; i++) {
            troubleShootService.request(getProject(), "GET", new MyInputData("<foo/>"), "defects/{0}", "0");
        }
        testApplication.waitForBackgroundActivityToFinish();
        // only 1 notification due to notification delay
        Assert.assertEquals(1, times.getValue());

        troubleShootService._setNotificationDelay(0);
        for(int i = 0; i < 100; i++) {
            troubleShootService.request(getProject(), "GET", new MyInputData("<foo/>"), "defects/{0}", "0");
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // additional 10 notifications
                    Assert.assertEquals(11, times.getValue());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        connection.disconnect();
        troubleShootService.stop();
    }
}
