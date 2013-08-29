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

package com.hp.alm.ali.idea.entity;

import java.util.Map;

public interface EntityFilter<E extends EntityFilter> {

    String getEntityType();

    Map<String, String> getPropertyMap();

    boolean setValue(String property, String value);

    String getValue(String property);

    void setPropertyResolved(String property, boolean resolved);

    boolean isResolved(String property);

    void clear();

    boolean isEmpty();

    void copyFrom(E other);

    E clone();

}
