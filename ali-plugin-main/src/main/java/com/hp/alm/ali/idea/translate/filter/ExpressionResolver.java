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

package com.hp.alm.ali.idea.translate.filter;

import com.hp.alm.ali.idea.translate.expr.ExpressionBuilder;
import com.hp.alm.ali.idea.translate.expr.ExpressionParser;
import com.hp.alm.ali.idea.translate.expr.Node;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;

public class ExpressionResolver implements FilterResolver {

    private Translator translator;

    public ExpressionResolver(Translator translator) {
        this.translator = translator;
    }

    @Override
    public String resolveDisplayValue(String expr, ValueCallback onValue) {
        Node node = ExpressionParser.parse(expr);
        String value;
        if(resolve(node, translator, onValue, node)) {
            value = ExpressionBuilder.build(node);
        } else {
            value = TranslateService.LOADING_MESSAGE;
        }
        onValue.value(value);
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        return value;
    }

    private boolean isResolved(Node node) {
        if(TranslateService.LOADING_MESSAGE.equals(node.value)) {
            return false;
        }

        return (node.left == null || isResolved(node.left)) && (node.right == null || isResolved(node.right));
    }

    private boolean resolve(final Node node, Translator translator, final ValueCallback onValue, final Node root) {
        if(node.value != null) {
            node.value = translator.translate(node.value, new ValueCallback() {
                @Override
                public void value(String value) {
                    node.value = value;
                    if (isResolved(root)) {
                        onValue.value(ExpressionBuilder.build(root));
                    }
                }
            });
            return !TranslateService.LOADING_MESSAGE.equals(node.value);
        }
        boolean resolved = true;
        if(node.left != null) {
            resolved = resolve(node.left, translator, onValue, root);
        }
        if(node.right != null) {
            resolved &= resolve(node.right, translator, onValue, root);
        }
        return resolved;
    }
}
