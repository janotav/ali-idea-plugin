/*
 * Copyright 2014 Hewlett-Packard Development Company, L.P
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

package com.hp.alm.ali.idea.spellcheck;

import java.text.BreakIterator;
import java.util.NoSuchElementException;

public class SpellCheckTokenizer {

    private BreakIterator wordIterator;
    private Token token;
    private String text;

    public SpellCheckTokenizer(String text) {
        this.text = text;

        wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(text);
        wordIterator.first();
    }

    public boolean hasMoreTokens() {
        while (token == null) {
            int offset = wordIterator.current();
            if (offset >= text.length()) {
                return false;
            }
            String word = text.substring(offset, wordIterator.next());
            if (Character.isLetter(word.charAt(0))) {
                // only words starting with letter (no whitespace, punctuation etc)
                token = new Token(word, offset);
            }
        }
        return true;
    }

    public Token nextToken() {
        if (!hasMoreTokens()) {
            throw new NoSuchElementException();

        }
        Token current = token;
        token = null;
        return current;
    }

    public static class Token {

        private final String word;
        private final int offset;

        Token(String word, int offset) {
            this.word = word;
            this.offset = offset;
        }

        public String getWord() {
            return word;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return word.length();
        }
    }
}
