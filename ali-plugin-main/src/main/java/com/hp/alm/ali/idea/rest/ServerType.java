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

package com.hp.alm.ali.idea.rest;

import com.hp.alm.ali.idea.model.Ali2Strategy;
import com.hp.alm.ali.idea.model.AliStrategy;
import com.hp.alm.ali.idea.model.ApolloStrategy;
import com.hp.alm.ali.idea.model.HorizonStrategy;
import com.hp.alm.ali.idea.model.MayaStrategy;
import com.hp.alm.ali.idea.model.ServerStrategy;

public enum ServerType {

    CONNECTING("Connecting..."),
    NONE("Not Connected"),
    NEEDS_PASSWORD("Needs Password"),
    ALM11("ALM 11", MayaStrategy.class, false),
    ALI("ALI 1.x", AliStrategy.class, false),
    ALI2("ALI 2.0", Ali2Strategy.class, false),
    ALM12("ALM12", ApolloStrategy.class, true),
    AGM("AGM", HorizonStrategy.class, true);

    private String name;
    private Class<? extends ServerStrategy> clazz;
    private boolean apollo; // TODO: remove

    private ServerType(String name) {
        this.name = name;
    }

    private ServerType(String name, Class<? extends ServerStrategy> clazz, boolean apollo) {
        this.name = name;
        this.clazz = clazz;
        this.apollo = apollo;
    }

    public String toString() {
        return name;
    }

    public boolean isConnected() {
        return clazz != null;
    }

    public boolean isApollo() {
        return apollo;
    }

    public Class<? extends ServerStrategy> getClazz() {
        if(clazz == null) {
            throw new NotConnectedException();
        } else {
            return clazz;
        }
    }
}
