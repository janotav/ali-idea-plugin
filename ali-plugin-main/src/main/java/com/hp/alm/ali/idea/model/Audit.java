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

package com.hp.alm.ali.idea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Audit {

    private String username;
    private Date date;
    private List<String[]> props;

    public Audit() {
        props = new ArrayList<String[]>();
    }

    public boolean hasProperties() {
        return !props.isEmpty();
    }

    public void setTime(Date date) {
        this.date = date;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void addProperty(String propertyLabel, String oldValue, String newValue) {
        props.add(new String[] { propertyLabel, oldValue, newValue });
    }

    public String getUsername() {
        return username;
    }

    public Date getDate() {
        return date;
    }

    public List<String[]> getProperties() {
        return Collections.unmodifiableList(props);
    }
}
