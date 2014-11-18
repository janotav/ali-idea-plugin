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

import org.junit.Assert;
import org.junit.Test;

public class SpellCheckTokenizerTest {

    @Test
    public void testSpellCheckTokenizer() {
        check("Gives words except 12345 or punctuation, and like.\n",
                new SpellCheckTokenizer.Token("Gives", 0),
                new SpellCheckTokenizer.Token("words", 6),
                new SpellCheckTokenizer.Token("except", 12),
                new SpellCheckTokenizer.Token("or", 25),
                new SpellCheckTokenizer.Token("punctuation", 28),
                new SpellCheckTokenizer.Token("and", 41),
                new SpellCheckTokenizer.Token("like", 45));
    }

    @Test
    public void testSpellCheckTokenizerEmpty() {
        check("");
        check("\n\n");
        check("12345 !@#$%");
    }


    private void check(String sentence, SpellCheckTokenizer.Token... tokens) {
        SpellCheckTokenizer tokenizer = new SpellCheckTokenizer(sentence);
        for (SpellCheckTokenizer.Token expected: tokens) {
            Assert.assertTrue(tokenizer.hasMoreTokens());
            SpellCheckTokenizer.Token token = tokenizer.nextToken();
            Assert.assertEquals(expected.getWord(), token.getWord());
            Assert.assertEquals(expected.getOffset(), token.getOffset());
        }
        Assert.assertFalse(tokenizer.hasMoreTokens());
    }
}
