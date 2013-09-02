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

package com.hp.alm.ali.rest.client.filter;

import com.hp.alm.ali.rest.client.ResultInfo;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueTicketFilter implements ResponseFilter {

    @Override
    public Filter applyFilter(Filter filter, HttpMethod method, ResultInfo resultInfo) {
        Header contentType = method.getResponseHeader("Content-type");
        if(method.getStatusCode() == 500 && contentType != null && contentType.getValue().contains("text/html")) {
            return new MyFilter(filter, resultInfo);
        } else {
            return filter;
        }
    }

    private static class MyFilter implements Filter {

        private Filter inner;
        private ResultInfo resultInfo;

        public MyFilter(Filter inner, ResultInfo resultInfo) {
            this.inner = inner;
            this.resultInfo = resultInfo;
        }

        @Override
        public OutputStream getOutputStream() {
            return new MyStream(inner.getOutputStream(), resultInfo);
        }
    }

    private static class MyStream extends OutputStream {

        private static Pattern pattern = Pattern.compile("<meta name=\"description\" content=\"([^\"]+)\" id=\"errorcode\"/>", Pattern.CASE_INSENSITIVE);

        private ByteArrayOutputStream baos;
        private OutputStream os;
        private ResultInfo resultInfo;

        public MyStream(OutputStream os, ResultInfo resultInfo) {
            this.os = os;
            this.resultInfo = resultInfo;
            this.baos = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            os.write(b);
            if(baos != null) {
                baos.write(b);
                check();
            }
        }

        @Override
        public void write(byte b[]) throws IOException {
            os.write(b);
            if(baos != null) {
                baos.write(b);
                check();
            }
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            os.write(b, off, len);
            if(baos != null) {
                baos.write(b, off, len);
                check();
            }
        }

        private void check() {
            String str = baos.toString();
            Matcher matcher = pattern.matcher(str);
            if(matcher.find()) {
                resultInfo.setErrorCode(matcher.group(1));
                baos = null;
            } else if(str.contains("</head>")) {
                baos = null;
            }
        }
    }
}
