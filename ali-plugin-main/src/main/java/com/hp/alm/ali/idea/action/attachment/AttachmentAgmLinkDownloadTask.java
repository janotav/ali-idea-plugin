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

package com.hp.alm.ali.idea.action.attachment;

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.AlmRememberedInputs;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.rest.client.AliRestClient;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.rest.client.ResultInfo;
import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.intellij.ide.passwordSafe.MasterPasswordUnavailableException;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.AuthData;
import com.intellij.vcsUtil.AuthDialog;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttachmentAgmLinkDownloadTask extends AttachmentDownloadTask {

    private static Logger logger = Logger.getInstance(AttachmentAgmLinkDownloadTask.class);

    private static Pattern pattern = Pattern.compile("(https?://[^/]+/qcbin)/rest/domains/([^/]+)/projects/([^/]+)/(.*)");

    private Project project;
    private String username;
    private File targetFile;
    private final Runnable onFinished;

    public AttachmentAgmLinkDownloadTask(Project project, File file, String sourceFilename, int size, EntityRef entity, Runnable onFinished) {
        super(project, createTempFile(), sourceFilename, size, entity, null);

        this.project = project;
        this.targetFile = file;
        this.onFinished = onFinished;

        AliProjectConfiguration configuration = project.getComponent(AliProjectConfiguration.class);
        username = configuration.getUsername().replace("@", "_");
    }

    public void run(ProgressIndicator indicator) {
        super.run(indicator);
        if (!file.exists()) {
            // download cancelled
            return;
        }
        try {
            String url = FileUtils.readFileToString(file, "UTF-8");
            // link file (no longer needed)
            file.delete();
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                final String location = matcher.group(1);
                AliRestClient almRestClient;

                String password = null;
                boolean persistDisk = false;
                AuthData authData = getAuthData(location);
                boolean hasAuthData = (authData != null);
                if (hasAuthData) {
                    username = authData.getLogin();
                    password = authData.getPassword();
                }
                while (true) {
                    if (!hasAuthData) {
                        AuthDialog dialog = getAuthDialog(location);
                        if (dialog == null) {
                            return;
                        }
                        username = dialog.getUsername();
                        password = dialog.getPassword();
                        persistDisk = dialog.isRememberPassword();
                    }
                    almRestClient = AliRestClient.create(location,
                            matcher.group(2),
                            matcher.group(3),
                            username,
                            password,
                            RestClient.SessionStrategy.AUTO_LOGIN);
                    try {
                        almRestClient.login();

                        if (!hasAuthData) {
                            saveAuthData(location, username, password, persistDisk);
                        }
                        break;
                    } catch (AuthenticationFailureException e) {
                        // wrong password
                        hasAuthData = false;
                    }
                }

                FileOutputStream fos = new FileOutputStream(targetFile);
                ResultInfo info = ResultInfo.create(fos);
                if (almRestClient.get(info, matcher.group(4)) == 200) {
                    fos.close();
                } else {
                    // remove the file if not successful
                    targetFile.delete();
                }
                try {
                    almRestClient.logout();
                } catch (Exception e) {
                    logger.warn("Logout failed", e);
                }
                if (onFinished != null) {
                    onFinished.run();
                }
            } else {
                logger.warn("Unexpected URL format: " + url);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthDialog getAuthDialog(final String location) {
        final Ref<AuthDialog> dialog = Ref.create();
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                dialog.set(new AuthDialog(project, "ALM credentials", "Enter credentials for " + location, username, null, true));
                dialog.get().show();
            }
        }, ModalityState.any());
        if (dialog.get().isOK()) {
            return dialog.get();
        } else {
            return null;
        }
    }

    private void saveAuthData(String location, String username, String password, boolean persistDisk) {
        AlmRememberedInputs.getInstance().addUrl(location, username);
        String key = makeKey(username, location);
        PasswordSafeImpl passwordSafe = (PasswordSafeImpl) PasswordSafe.getInstance();
        try {
            passwordSafe.getMemoryProvider().storePassword(project, AttachmentAgmLinkDownloadTask.class, key, password);
            if (persistDisk) {
                passwordSafe.getMasterKeyProvider().storePassword(project, AttachmentAgmLinkDownloadTask.class, key, password);
            }
        }
        catch (MasterPasswordUnavailableException e) {
            logger.error("Couldn't remember password for " + key, e);
        }
        catch (PasswordSafeException e) {
            logger.error("Couldn't remember password for " + key, e);
        }
    }

    private AuthData getAuthData(String url) {
        String userName = AlmRememberedInputs.getInstance().getUserNameForUrl(url);
        String key = makeKey(userName, url);
        final PasswordSafe passwordSafe = PasswordSafe.getInstance();
        try {
            String password = passwordSafe.getPassword(project, AttachmentAgmLinkDownloadTask.class, key);
            return new AuthData(StringUtil.notNullize(userName), password);
        }
        catch (PasswordSafeException e) {
            logger.error("Couldn't get the password for key [" + key + "]", e);
            return null;
        }
    }

    private String makeKey(String username, String url) {
        return username + "@" + url;
    }

    private static File createTempFile() {
        try {
            File tempFile = File.createTempFile("agmlink", "");
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
