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

package com.hp.alm.ali.rest.client.exception;

abstract public class HttpStatusBasedException extends AliRestException {
    private final int httpStatus;
    private final String location;


    protected HttpStatusBasedException(int status, String location) {
        super("status : " + status + " location : " + location);
        httpStatus = status;
        this.location = location;
    }

    public static HttpStatusBasedException of(int statusCode, String location) {
        switch (statusCode) {
            case 401:
                return new AuthenticationFailureException(location);
            default:
                if (statusCode >= 500 && statusCode <= 599) return new HttpServerErrorException(statusCode, location);
                if (statusCode >= 400 && statusCode <= 499) return new HttpClientErrorException(statusCode, location);
                return null;
        }
    }

    public static void throwForError(int statusCode, String location) {
        HttpStatusBasedException exception = of(statusCode, location);
        if (exception != null) throw exception;
    }


    public int getHttpStatus() {
        return httpStatus;
    }

    public String getLocation() {
        return location;
    }
}
