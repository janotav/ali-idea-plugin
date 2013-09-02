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

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides information extracted from HTTP response.
 */
public class ResultInfo {
    private int httpStatus;
    private final Map<String, String> headers;
    private final OutputStream bodyStream;
    private String location;
    private String reasonPhrase;

    private ResultInfo(OutputStream bodyStream) {
        this.headers = new HashMap<String, String>();
        this.bodyStream = bodyStream;
    }

    /**
     * Creates result info to be used as container for HTTP response related information.
     *
     * @param responseBody target stream to write http response body , stream is closed after response is written, {@code null} means that response body is dropped
     * @return result info
     */
    public static ResultInfo create(OutputStream responseBody) {
        return new ResultInfo(responseBody);
    }

    /**
     * Returns status code of performed http operation.
     *
     * @return status code
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Map of response headers.
     *
     * @return response headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    public OutputStream getBodyStream() {
        return bodyStream;
    }

    public String getLocation() {
        return location;
    }

    public String getErrorCode() {
        return headers.get("error_code");
    }

    public void setErrorCode(String errorCode) {
        headers.put("error_code", errorCode);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }
}


