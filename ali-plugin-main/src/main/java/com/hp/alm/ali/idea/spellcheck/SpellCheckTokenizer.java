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
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class SpellCheckTokenizer {

    private LinkedList<String> words;
    private Token token;
    private int offset;

    public SpellCheckTokenizer(String text) {
        words = new LinkedList<String>();

        BreakIterator wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(text);
        wordIterator.first();

        // break text to words
        while (true) {
            int offset = wordIterator.current();
            if (offset == BreakIterator.DONE || offset >= text.length()) {
                break;
            }
            int next = wordIterator.next();
            words.add(text.substring(offset, next == BreakIterator.DONE? text.length() - offset: next));
        }
    }

    public boolean hasMoreTokens() {
        while (true) {
            if (token != null) {
                return true;
            }
            while (!words.isEmpty()) {
                String word = words.getFirst();
                if (!Character.isLetter(word.charAt(0))) {
                    // discard words not starting with letter (whitespace, punctuation etc)
                    offset += word.length();
                    words.removeFirst();
                } else {
                    break;
                }
            }
            if (!words.isEmpty()) {
                token = new Token(words.removeFirst(), offset);
                offset += token.getLength();
            } else {
                return false;
            }
        }
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
