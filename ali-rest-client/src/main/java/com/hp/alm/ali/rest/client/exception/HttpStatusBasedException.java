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

import com.hp.alm.ali.rest.client.ResultInfo;

abstract public class HttpStatusBasedException extends RuntimeException {
    private final int httpStatus;
    private final String reasonPhrase;
    private final String location;
    private final String errorCode;

    protected HttpStatusBasedException(ResultInfo resultInfo) {
        super(message(resultInfo));
        httpStatus = resultInfo.getHttpStatus();
        reasonPhrase = resultInfo.getReasonPhrase();
        location = resultInfo.getLocation();
        errorCode = resultInfo.getErrorCode();
    }

    public static void throwForError(ResultInfo resultInfo) {
        int statusCode = resultInfo.getHttpStatus();
        switch (statusCode) {
            case 401:
                throw new AuthenticationFailureException(resultInfo);
            default:
                if (statusCode >= 500 && statusCode <= 599) {
                    throw new HttpServerErrorException(resultInfo);
                }
                if (statusCode >= 400 && statusCode <= 499) {
                    throw new HttpClientErrorException(resultInfo);
                }
        }
    }

    private static String message(ResultInfo resultInfo) {
        if(resultInfo.getErrorCode() != null) {
            return resultInfo.getHttpStatus() + " " + resultInfo.getReasonPhrase() + " [location: " + resultInfo.getLocation() + "; error_code: " + resultInfo.getErrorCode() + "]";
        } else {
            return resultInfo.getHttpStatus() + " " + resultInfo.getReasonPhrase() + " [location: " + resultInfo.getLocation() + "]";
        }
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getLocation() {
        return location;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
