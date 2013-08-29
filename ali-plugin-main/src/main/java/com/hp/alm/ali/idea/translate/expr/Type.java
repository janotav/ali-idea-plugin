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

package com.hp.alm.ali.idea.translate.expr;

public enum Type {
    VALUE(null),
    AND,
    OR,
    NOT,
    LTE("<="),
    GTE(">="),
    DIFFERS("<>"),
    LT("<"),
    GT(">"),
    EQ("="),
    LEFT_P("("),
    RIGHT_P(")"),
    END(null);

    private String repr;

    Type() {
        this.repr = name();
    }

    Type(String repr) {
        this.repr = repr;
    }

    String getRepr() {
        return repr;
    }
}
