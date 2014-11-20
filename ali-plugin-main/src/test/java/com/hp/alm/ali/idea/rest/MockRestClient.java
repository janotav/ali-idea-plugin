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

import com.hp.alm.ali.rest.client.InputData;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.rest.client.ResultInfo;
import org.apache.commons.httpclient.Cookie;
import org.junit.Assert;

import java.io.InputStream;
import java.util.List;

public class MockRestClient implements RestClient {

    @Override
    public SessionStrategy getSessionStrategy() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public void setEncoding(String encoding) {
        Assert.fail("Not expected");
    }

    @Override
    public String getEncoding() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public void setTimeout(int timeout) {
        Assert.fail("Not expected");
    }

    @Override
    public void setHttpProxy(String proxyHost, int proxyPort) {
        Assert.fail("Not expected");
    }

    @Override
    public void setHttpProxyCredentials(String username, String password) {
        Assert.fail("Not expected");
    }

    @Override
    public void login() {
        Assert.fail("Not expected");
    }

    @Override
    public void logout() {
        Assert.fail("Not expected");
    }

    @Override
    public void setDomain(String domain) {
        Assert.fail("Not expected");
    }

    @Override
    public String getDomain() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public void setProject(String project) {
        Assert.fail("Not expected");
    }

    @Override
    public String getProject() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public List<Cookie> getCookies(String name) {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public List<String> listDomains() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public List<String> listCurrentProjects() {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public int get(ResultInfo result, String template, Object... params) {
        Assert.fail("Not expected");
        return 0;
    }

    @Override
    public int put(InputData inputData, ResultInfo result, String template, Object... params) {
        Assert.fail("Not expected");
        return 0;
    }

    @Override
    public int post(InputData input, ResultInfo result, String template, Object... params) {
        Assert.fail("Not expected");
        return 0;
    }

    @Override
    public int delete(ResultInfo result, String template, Object... params) {
        Assert.fail("Not expected");
        return 0;
    }

    @Override
    public String getForString(String template, Object... params) {
        Assert.fail("Not expected");
        return null;
    }

    @Override
    public InputStream getForStream(String template, Object... params) {
        Assert.fail("Not expected");
        return null;
    }
}
