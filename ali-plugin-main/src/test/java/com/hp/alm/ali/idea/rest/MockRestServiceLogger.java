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
import com.intellij.openapi.project.Project;
import org.junit.Assert;

public class MockRestServiceLogger implements RestServiceLogger {

    @Override
    public long request(Project project, String name, MyInputData myInput, String template, Object... params) {
        Assert.fail("Not expected");
        return 0;
    }

    @Override
    public void loginFailure(long id, AuthenticationFailureException e) {
        Assert.fail("Not expected");
    }

    @Override
    public void response(long id, int code, MyResultInfo myResult) {
        Assert.fail("Not expected");
    }
}
