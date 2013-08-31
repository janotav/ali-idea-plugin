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

import java.util.Arrays;

public class TestSettings {

    public static ServerVersion getExecutionVersion() {
        String property = getRequiredProperty("SERVER_VERSION", "; valid values: " + Arrays.asList(ServerVersion.values()));
        try {
            return ServerVersion.valueOf(property);
        } catch (Exception e) {
            throw new RuntimeException("Invalid SERVER_VERSION specified; valid values: " + Arrays.asList(ServerVersion.values()));
        }
    }

    public static String getServerUrl() {
        return getRequiredProperty("SERVER_URL", "; e.g. http://localhost:8080/qcbin");
    }

    public static String getDomain() {
        return getRequiredProperty("DOMAIN");
    }

    public static String getProject() {
        return getRequiredProperty("PROJECT");
    }

    public static String getUsername() {
        return getRequiredProperty("USERNAME");
    }

    public static String getPassword() {
        return getRequiredProperty("PASSWORD");
    }

    public static String getRequiredProperty(String property, String customMessage) {
        String value = System.getProperty(property);
        if(value == null) {
            throw new RuntimeException("You need to specify -D" + property + "=..." + customMessage);
        }
        return value;
    }

    public static String getRequiredProperty(String property) {
        return getRequiredProperty(property, "");
    }
}
