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

package com.hp.alm.ali.idea.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transitions {

    public static final String DEFAULT_STATUS_TRANSITION  = "New:Open;Open:Fixed,Rejected;Fixed:Open,Reopen,Closed;Rejected:Open,Reopen,Closed;Reopen:Fixed,Rejected";

    private Map<String, List<String>> map;

    public Transitions(String allowed) {
        if(allowed.isEmpty()) {
            allowed = DEFAULT_STATUS_TRANSITION;
        }
        map = new HashMap<String, List<String>>();
        String[] segments = allowed.split(";");
        for(String segment: segments) {
            int p = segment.indexOf(":");
            if(p > 0) {
                String state = segment.substring(0, p).trim();
                map.put(state, Arrays.asList(segment.substring(p + 1).trim().split("\\s*,\\s*")));
            }
        }
    }

    public List<String> getAllowedTransitions(String state) {
        List<String> allowed = map.get(state);
        return allowed == null? Collections.<String>emptyList(): allowed;
    }
}
