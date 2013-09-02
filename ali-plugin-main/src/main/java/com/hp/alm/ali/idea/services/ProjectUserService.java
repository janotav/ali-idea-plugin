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

package com.hp.alm.ali.idea.services;

import com.hp.alm.ali.idea.model.User;
import com.hp.alm.ali.idea.model.parser.UserList;
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Transform;

import java.io.InputStream;

public class ProjectUserService extends AbstractCachingService<Integer, UserList, AbstractCachingService.Callback<UserList>> {
    private RestService restService;

    public ProjectUserService(Project project) {
        super(project);

        restService = project.getComponent(RestService.class);
    }

    public void loadUsersAsync(Callback<UserList> callback) {
        getValueAsync(1, callback);
    }

    public User tryGetUser(String username) {
        UserList users = getCachedValue(1);
        if(users != null) {
            User user = users.getUser(username);
            if(user == null) {
                // avoid infinite loop if users loaded but username not found
                return new User(username, username);
            }
            return user;
        } else {
            return null;
        }
    }

    public void loadUserAsync(final String username, final Callback<User> callback) {
        loadUsersAsync(translate(callback, new Transform<UserList, User>() {
            @Override
            public User transform(UserList users) {
                User user = users.getUser(username);
                if(user == null) {
                    return new User(username, username);
                } else {
                    return user;
                }
            }
        }));
    }

    public UserList getUserList() {
        return getValue(1);
    }

    public User getUser(String username) {
        for(User user: getUserList()) {
            if(user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    protected UserList doGetValue(Integer key) {
        InputStream is = restService.getForStream("customization/users");
        return UserList.create(is);
    }
}
