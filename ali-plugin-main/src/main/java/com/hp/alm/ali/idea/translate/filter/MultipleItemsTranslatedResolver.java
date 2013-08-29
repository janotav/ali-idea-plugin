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

import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultipleItemsTranslatedResolver extends MultipleItemsResolver {

    public static final String NO_VALUE = "\"\"";
    public static final String NO_VALUE_DESC = "(no value)";

    private Translator translator;

    public MultipleItemsTranslatedResolver(Translator translator) {
        this.translator = translator;
    }

    @Override
    public String resolveDisplayValue(String value, final ValueCallback onValue) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        String[] split = value.split(";");
        final int fCount = new HashSet<String>(Arrays.asList(split)).size();
        for(final String idStr: split) {
            if(NO_VALUE.equals(idStr)) {
                setValue(fCount, map, idStr, MultipleItemsTranslatedResolver.NO_VALUE_DESC, onValue);
                continue;
            }
            String result = translator.translate(idStr, new ValueCallback() {
                @Override
                public void value(String value) {
                    setValue(fCount, map, idStr, value, onValue);
                }
            });
            if(result != null) {
                    setValue(fCount, map, idStr, result, onValue);
            }
        }
        if(map.size() == fCount) {
            return StringUtils.join(map.values(), ";");
        } else {
            return "Loading...";
        }
    }

    private void setValue(int count, Map<String, String> map, String key, String value, ValueCallback onValue) {
        map.put(key, value);
        if(map.size() == count) {
            onValue.value(StringUtils.join(map.values(), ";"));
        }
    }
}
