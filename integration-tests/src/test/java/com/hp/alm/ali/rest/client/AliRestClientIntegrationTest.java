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

import com.hp.alm.ali.TestSettings;
import junit.framework.Assert;
import org.junit.Test;

/**
 * This test doesn't extend IntegrationTest because it doesn't depend on the Intellij runtime.
 */
public class AliRestClientIntegrationTest {

    @Test
    public void testLoginLogout() {
        AliRestClient client = createClient(AliRestClient.SessionStrategy.NONE);
        client.login();
        client.logout();
    }

    @Test
    public void testGetZeroDefectImplicitLogin() {
        AliRestClient client = createClient(AliRestClient.SessionStrategy.AUTO_LOGIN);
        String result = client.getForString("defects?query={0}", "{id[0]}");
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entities TotalResults=\"0\"/>", result);
        client.logout();
    }

    private AliRestClient createClient(AliRestClient.SessionStrategy strategy) {
        return AliRestClient.create(
                TestSettings.getServerUrl(),
                TestSettings.getDomain(),
                TestSettings.getProject(),
                TestSettings.getUsername(),
                TestSettings.getPassword(),
                strategy);
    }
}
