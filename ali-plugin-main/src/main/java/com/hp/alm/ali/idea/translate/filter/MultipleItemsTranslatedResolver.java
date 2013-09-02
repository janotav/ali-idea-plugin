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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class MultipleItemsTranslatedResolver extends MultipleItemsResolver {

    private Translator translator;

    public MultipleItemsTranslatedResolver(Translator translator) {
        this.translator = translator;
    }

    @Override
    public String resolveDisplayValue(String value, final ValueCallback onValue) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        final String[] split = value.split(";");
        synchronized (map) { // avoid unstable tests by enforcing order
            for(final String idStr: split) {
                if(NO_VALUE.equals(idStr)) {
                    setValue(split, map, idStr, NO_VALUE_DESC, onValue);
                    continue;
                }
                String result = translator.translate(idStr, new ValueCallback() {
                    @Override
                    public void value(String value) {
                        setValue(split, map, idStr, value, onValue);
                    }
                });
                if(result != null) {
                    setValue(split, map, idStr, result, onValue);
                }
            }
            String resolvedValue;
            if(map.size() == split.length) {
                resolvedValue = resolve(split, map);
            } else {
                resolvedValue = "Loading...";
            }
            onValue.value(resolvedValue);
            return resolvedValue;
        }
    }

    private String resolve(String[] split, Map<String, String> map) {
        // keep order (don't join values directly)
        LinkedList<String> list = new LinkedList<String>();
        for(String idStr: split) {
            list.add(map.get(idStr));
        }
        return StringUtils.join(list, ";");
    }

    private void setValue(String[] split, Map<String, String> map, String key, String value, ValueCallback onValue) {
        synchronized (map) {
            map.put(key, value);
            if(map.size() == split.length) {
                onValue.value(resolve(split, map));
            }
        }
    }
}
