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

import com.hp.alm.ali.idea.progress.IndicatingInputStream;
import com.hp.alm.ali.rest.client.InputData;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class MyInputData {

    private InputData data;
    private ByteArrayOutputStream copy;
    private Map<String, String> headers;
    private String content;

    public MyInputData(String xml) {
        data = InputData.create(xml);

        this.content = xml;
    }

    public MyInputData(String xml, Map<String, String> headers) {
        data = InputData.create(xml, headers);

        this.content = xml;
        this.headers = headers;
    }

    public MyInputData(IndicatingInputStream is, long length, Map<String, String> headers) {
        data = InputData.create(is, length, headers);

        copy = new ByteArrayOutputStream();
        is.addObserver(copy);

        this.headers = headers;
    }

    public InputData getInputData() {
        return data;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestData() {
        if(content != null) {
            return content;
        } else if(copy != null) {
            return new String(copy.toByteArray());
        } else {
            return null;
        }
    }
}
