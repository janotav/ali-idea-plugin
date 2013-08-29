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

public class ExpressionParser {

    public static Node parse(String expr) {
        Lexer lexer = new Lexer(expr);
        return matchExpr(lexer);
    }

    private static Node matchExpr(Lexer lexer) {
        Node result = matchE(lexer);
        if(lexer.next().type != Type.END) {
            throw new IllegalArgumentException("Trailing characters at position "+lexer.getPosition()+": "+lexer.getRemaining());
        }
        return result;
    }

    private static Node matchE(Lexer lexer) {
        Node result = matchT(lexer);
        if(lexer.next().type == Type.AND || lexer.next().type == Type.OR) {
            Lexeme lexeme = lexer.consume();
            return new Node(lexeme.type, result, matchE(lexer));
        }
        return result;
    }

    private static Node matchT(Lexer lexer) {
        if(lexer.next().type == Type.NOT) {
            Lexeme lexeme = lexer.consume();
            return new Node(lexeme.type, matchT(lexer));
        }
        return matchQ(lexer);
    }

    private static Node matchQ(Lexer lexer) {
        Lexeme next = lexer.next();
        switch (next.type) {
            case EQ:
            case LT:
            case LTE:
            case GT:
            case GTE:
            case DIFFERS:
                lexer.consume();
                return new Node(next.type, matchF(lexer));
        }
        return matchF(lexer);
    }

    private static Node matchF(Lexer lexer) {
        Lexeme lexeme = lexer.consume();
        if(lexeme.type == Type.VALUE) {
            return new Node(lexeme.value);
        } else if(lexeme.type == Type.LEFT_P) {
            Node result = matchE(lexer);
            lexer.expect(Type.RIGHT_P);
            return result;
        }
        throw new IllegalArgumentException("Expected VALUE or '(' at position "+lexeme.start+": "+lexeme.value);
    }
}
