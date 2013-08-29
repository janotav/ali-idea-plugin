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

import com.hp.alm.ali.idea.progress.IndicatingOutputStream;
import com.hp.alm.ali.rest.client.ResultInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MyResultInfo {

    private OutputStream os;
    private ByteArrayOutputStream copy;
    private Map<String, String> headers;
    private String location;

    public MyResultInfo() {
        copy = new ByteArrayOutputStream();
        os = copy;

        headers = new HashMap<String, String>();
    }

    public MyResultInfo(IndicatingOutputStream ios) {
        os = ios;
        copy = new ByteArrayOutputStream();
        ios.addObserver(copy);

        headers = new HashMap<String, String>();
    }


    public OutputStream getOutputStream() {
        return os;
    }

    public String getBodyAsString() {
        return new String(copy.toByteArray());
    }

    public InputStream getBodyAsStream() {
        return new ByteArrayInputStream(copy.toByteArray());
    }

    public void copyFrom(ResultInfo info) {
        headers.putAll(info.getHeaders());
        location = info.getLocation();
    }

    public String getLocation() {
        return location;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
