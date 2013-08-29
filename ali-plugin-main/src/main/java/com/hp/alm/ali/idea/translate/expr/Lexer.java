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

import java.util.NoSuchElementException;

class Lexer {

    private String str;
    private int pos;
    private Lexeme next;

    Lexer(String str) {
        this.str = str;
        this.pos = 0;
    }

    Lexeme next() {
        if(next == null) {
            while(pos < str.length() && Character.isWhitespace(str.charAt(pos))) {
                ++pos;
            }
            if(pos > str.length()) {
                throw new NoSuchElementException();
            }
            if(pos == str.length()) {
                next = new Lexeme(pos, pos, "", Type.END);
                return next;
            }
            int startPos = pos;
            if(str.charAt(pos) == '\'' || str.charAt(pos) == '"') {
                int end = pos;
                do {
                    ++end;
                } while(end < str.length() && str.charAt(end) != str.charAt(pos));
                if(pos == end || str.charAt(end) != str.charAt(pos)) {
                    throw new IllegalArgumentException("Unterminated literal at position "+pos+": "+str.substring(pos));
                }
                pos = end + 1;
                next = new Lexeme(startPos, end, str.substring(startPos + 1, end), Type.VALUE);
                return next;
            }
            for(Type type: Type.values()) {
                if(type.getRepr() == null) {
                    continue;
                }

                if(str.substring(pos).toUpperCase().startsWith(type.getRepr())) {
                    next = new Lexeme(startPos, startPos + type.getRepr().length(), str.substring(startPos, startPos + type.getRepr().length()), type);
                    pos += type.getRepr().length();
                    return next;
                }
            }
            do {
                ++pos;
            } while(pos < str.length() && allowedInLiteral(str.charAt(pos)));
            next = new Lexeme(startPos, pos, str.substring(startPos, pos), Type.VALUE);
        }
        return next;
    }

    private boolean allowedInLiteral(char c) {
        return !Character.isWhitespace(c) && c != '(' && c != ')' && c != '<' && c != '>' && c != '=';
    }

    Lexeme consume() {
        Lexeme lexeme = next();
        next = null;
        return lexeme;
    }

    Lexeme expect(Type type) {
        Lexeme lexeme = next();
        if(lexeme.type != type) {
            throw new IllegalStateException("Expected "+type+" at position "+pos+": "+str.substring(pos));
        }
        next = null;
        return lexeme;
    }


    int getPosition() {
        return pos;
    }

    String getRemaining() {
        return str.substring(pos);
    }
}
