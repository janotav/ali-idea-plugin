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

package com.hp.alm.ali.idea.navigation;

public interface TestRecognizer {

    Result evaluate(String path, String href);

    public final static class Result {

        private String name;
        private String className;

        public Result(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }
    }
}
