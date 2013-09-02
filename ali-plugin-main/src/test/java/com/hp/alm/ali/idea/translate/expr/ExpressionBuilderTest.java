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

public class ExpressionBuilderTest {

    @Test
    public void testBuildValue() {
        String str = ExpressionBuilder.build(new Node("abc"));
        Assert.assertEquals("abc", str);
    }

    @Test
    public void testBuildAnd() {
        String str = ExpressionBuilder.build(new Node(Type.AND, new Node("abc"), new Node("def")));
        Assert.assertEquals("(abc AND def)", str);
    }

    @Test
    public void testBuildOr() {
        String str = ExpressionBuilder.build(new Node(Type.OR, new Node("abc"), new Node("def")));
        Assert.assertEquals("(abc OR def)", str);
    }

    @Test
    public void testBuildNot() {
        String str = ExpressionBuilder.build(new Node(Type.NOT, new Node("abc")));
        Assert.assertEquals("NOT abc", str);
    }

    @Test
    public void testBuildGt() {
        String str = ExpressionBuilder.build(new Node(Type.GT, new Node("abc")));
        Assert.assertEquals("> abc", str);
    }

    @Test
    public void testBuildGte() {
        String str = ExpressionBuilder.build(new Node(Type.GTE, new Node("abc")));
        Assert.assertEquals(">= abc", str);
    }

    @Test
    public void testBuildLt() {
        String str = ExpressionBuilder.build(new Node(Type.LT, new Node("abc")));
        Assert.assertEquals("< abc", str);
    }

    @Test
    public void testBuildLte() {
        String str = ExpressionBuilder.build(new Node(Type.LTE, new Node("abc")));
        Assert.assertEquals("<= abc", str);
    }

    @Test
    public void testBuildDiffers() {
        String str = ExpressionBuilder.build(new Node(Type.DIFFERS, new Node("abc")));
        Assert.assertEquals("<> abc", str);
    }

    @Test
    public void testBuildEq() {
        String str = ExpressionBuilder.build(new Node(Type.EQ, new Node("abc")));
        Assert.assertEquals("= abc", str);
    }

    @Test
    public void testBuildInvalid() {
        try {
            ExpressionBuilder.build(new Node(Type.LEFT_P, new Node("abc")));
        } catch (Exception e) {
            Assert.assertEquals("Unexpected node type: LEFT_P", e.getMessage());
        }
    }
}
