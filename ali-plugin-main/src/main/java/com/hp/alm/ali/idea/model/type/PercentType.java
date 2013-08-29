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

package com.hp.alm.ali.idea.model.type;

import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.translate.expr.ExpressionBuilder;
import com.hp.alm.ali.idea.translate.expr.ExpressionParser;
import com.hp.alm.ali.idea.translate.expr.Node;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;

public class PercentType implements Type, Translator, FilterResolver {

    @Override
    public FilterFactory getFilterFactory(boolean multiple) {
        return null;
    }

    @Override
    public FilterResolver getFilterResolver() {
        return this;
    }

    @Override
    public Translator getTranslator() {
        return this;
    }

    @Override
    public String translate(String value, ValueCallback callback) {
        value = value + "%";
        callback.value(value);
        return value;
    }

    @Override
    public String resolveDisplayValue(String value, ValueCallback onValue) {
        onValue.value(value);
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        Node node = ExpressionParser.parse(value);
        removePct(node);
        return ExpressionBuilder.build(node);
    }

    private void removePct(Node node) {
        if(node.value != null) {
            node.value = node.value.replaceFirst("%$", "");
        }
        if(node.left != null) {
            removePct(node.left);
        }
        if(node.right != null) {
            removePct(node.right);
        }
    }
}
