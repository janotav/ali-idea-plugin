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

package com.hp.alm.ali.idea.util;

import com.hp.alm.ali.idea.IdeaCompatibility;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class IdeaCompatibilityImpl implements IdeaCompatibility {

    private Project project;
    private Map<Class, Pair<Class, Integer>> map;
    private int runtimeBaseline;

    public IdeaCompatibilityImpl(Project project) {
        this.project = project;
        map = new HashMap<Class, Pair<Class, Integer>>();
        runtimeBaseline = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
    }

    @Override
    public boolean register(Class inf, Class impl, int baseline) {
        if(baseline <= runtimeBaseline) {
            Pair<Class, Integer> pair = map.get(inf);
            if(pair == null || pair.getSecond() < baseline) {
                map.put(inf, new Pair<Class, Integer>(impl, baseline));
                return true;
            }
        }
        return false;
    }

    @Override
    public <E> E getComponent(Class<E> inf) {
        return (E)project.getComponent(getComponentClass(inf));
    }

    private Class getComponentClass(Class clazz) {
        return map.get(clazz).getFirst();
    }
}
