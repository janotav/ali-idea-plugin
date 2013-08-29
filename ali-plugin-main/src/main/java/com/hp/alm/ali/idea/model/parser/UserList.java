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

package com.hp.alm.ali.idea.model.parser;

import com.hp.alm.ali.idea.model.User;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UserList extends AbstractList<User> {

    public static UserList create(InputStream is) {
        try {
            return new UserList(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private UserList(InputStream is) throws XMLStreamException {
        super(is);
    }

    protected void onStartElement(StartElement element) {
        String localPart = element.getName().getLocalPart();
        if("User".equals(localPart)) {
            String username = element.getAttributeByName(new QName(null, "Name")).getValue();
            String fullName = element.getAttributeByName(new QName(null, "FullName")).getValue();
            add(new User(username, fullName));
        }
    }

    public List<String> listNames() {
        ArrayList<String> list = new ArrayList<String>(size());
        for(User user: this) {
            list.add(user.getUsername());
        }
        return list;
    }

    public User getUser(String username) {
        for(User user: this) {
            if(username.equals(user.getUsername())) {
                return user;
            }
        }
        return null;
    }
}
