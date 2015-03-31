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
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExpressionResolver implements FilterResolver {

    private Translator translator;

    public ExpressionResolver(Translator translator) {
        this.translator = translator;
    }

    @Override
    public String resolveDisplayValue(String expr, ValueCallback onValue) {
        Node node = ExpressionParser.parse(expr);
        String value;
        SingleValueCallBack callBack = new SingleValueCallBack(onValue);
        if(resolve(node, translator, callBack, node, new HashSet<Node>(Collections.singleton(node)))) {
            value = ExpressionBuilder.build(node);
            callBack.value(value);
        } else {
            value = TranslateService.LOADING_MESSAGE;
            callBack.initValue(value);
        }
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        return value;
    }

    public Translator getTranslator() {
        return translator;
    }

    private boolean isResolved(Node node) {
        if(TranslateService.LOADING_MESSAGE.equals(node.value)) {
            return false;
        }

        return (node.left == null || isResolved(node.left)) && (node.right == null || isResolved(node.right));
    }

    private boolean resolve(final Node node, Translator translator, final ValueCallback onValue, final Node root, final Set<Node> toResolve) {
        if(node.value != null) {
            final Object lock = new Object();
            synchronized (lock) {
                node.value = translator.translate(node.value, new ValueCallback() {
                    @Override
                    public void value(String value) {
                        synchronized (lock)  {
                            node.value = value;
                            resolved(node, onValue, root, toResolve);
                        }
                    }
                });
                if(node.value == null) {
                    node.value = TranslateService.LOADING_MESSAGE;
                    return false;
                } else {
                    resolved(node, onValue, root, toResolve);
                    return true;
                }
            }
        }
        boolean leftResolved = true;
        if(node.left != null) {
            toResolve.add(node.left);
            leftResolved = resolve(node.left, translator, onValue, root, toResolve);
        }
        boolean rightResolved = true;
        if(node.right != null) {
            toResolve.add(node.right);
            rightResolved = resolve(node.right, translator, onValue, root, toResolve);
        }
        resolved(node, onValue, root, toResolve);
        return leftResolved && rightResolved;
    }

    private void resolved(Node node, ValueCallback onValue, Node root, Set<Node> toResolve) {
        synchronized (toResolve) {
            toResolve.remove(node);
            if (toResolve.isEmpty()) {
                onValue.value(ExpressionBuilder.build(root));
            }
        }
    }

    private static class SingleValueCallBack implements ValueCallback {

        private ValueCallback delegate;

        public SingleValueCallBack(ValueCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void value(String value) {
            if (delegate != null) {
                delegate.value(value);
                delegate = null;
            }
        }

        public synchronized void initValue(String value) {
            if (delegate != null) {
                delegate.value(value);
            }
        }
    }
}
