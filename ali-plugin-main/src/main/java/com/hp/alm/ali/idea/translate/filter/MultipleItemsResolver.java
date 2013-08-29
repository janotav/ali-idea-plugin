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

import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.translate.ValueCallback;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public class MultipleItemsResolver implements FilterResolver {

    @Override
    public String resolveDisplayValue(String value, final ValueCallback onValue) {
        value = value.replace(MultipleItemsTranslatedResolver.NO_VALUE, MultipleItemsTranslatedResolver.NO_VALUE_DESC);
        onValue.value(value);
        return value;
    }

    @Override
    public String toRESTQuery(String value) {
        List<String> values = Arrays.asList(value.split(";"));
        return StringUtils.join(EntityTableModel.quote(values), " OR ");
    }
}
