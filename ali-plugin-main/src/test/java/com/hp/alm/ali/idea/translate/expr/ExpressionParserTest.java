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

import org.junit.Assert;
import org.junit.Test;

public class ExpressionParserTest {

    @Test
    public void testParseBasics() {
        ExpressionParser.parse("value");
        ExpressionParser.parse("value AND value");
        ExpressionParser.parse("value OR value");
        ExpressionParser.parse("NOT value");
        ExpressionParser.parse("(value)");
        ExpressionParser.parse("'my value'");
        ExpressionParser.parse("\"my value\"");
        ExpressionParser.parse("<value");
        ExpressionParser.parse("<=value");
        ExpressionParser.parse("<>value");
        ExpressionParser.parse("=value");
        ExpressionParser.parse(">value");
        ExpressionParser.parse(">=value");
        ExpressionParser.parse("(> A OR (<= \"B C\" AND NOT ='D E'))");
    }


    @Test
    public void testParseFailure() {
        negative("my value", "Trailing characters at position 4");
        negative("value AND", "Expected VALUE at position 10");
        negative("value AND OR", "Expected VALUE at position 11");
        negative("(my value)", "Expected ) at position 5");
        negative("value)", "Trailing characters at position 6");
        negative("'value", "Unterminated literal at position 1");
        negative("value\"", "Unterminated literal at position 6");
        negative("'value\"", "Unterminated literal at position 1");
        negative("< (a or b)", "Expected VALUE at position 3");
    }

    @Test
    public void testDontMatchOperatorPrefix() {
        ExpressionParser.parse("orisprefixnotoperator");
    }

    @Test
    public void testOperatorPriorityLikeQc() {
        Node node = ExpressionParser.parse("<a OR >=b AND c");
        Assert.assertEquals("((< a OR >= b) AND c)", ExpressionBuilder.build(node));

        node = ExpressionParser.parse("<a OR (>=b AND c)");
        Assert.assertEquals("(< a OR (>= b AND c))", ExpressionBuilder.build(node));

        node = ExpressionParser.parse("<a AND >=b OR c");
        Assert.assertEquals("((< a AND >= b) OR c)", ExpressionBuilder.build(node));

        node = ExpressionParser.parse("<a AND (>=b OR c)");
        Assert.assertEquals("(< a AND (>= b OR c))", ExpressionBuilder.build(node));

        node = ExpressionParser.parse("(a AND b AND c) OR (x AND y AND z)");
        Assert.assertEquals("(((a AND b) AND c) OR ((x AND y) AND z))", ExpressionBuilder.build(node));
    }

    private void negative(String expr, String error) {
        try {
            ExpressionParser.parse(expr);
            Assert.fail("Fail expected");
        } catch (ParserException e) {
            Assert.assertEquals(error, e.getMessage());
        }
    }
}
