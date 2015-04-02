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

import com.hp.alm.ali.idea.content.devmotive.Commit;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.rest.RestService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevMotiveService {

    private RestService restService;

    public DevMotiveService(RestService restService) {
        this.restService = restService;
    }

    public Map<Commit, List<EntityRef>> getRelatedWorkItems(List<Commit> commits) {
        HashMap<Commit, List<EntityRef>> ret = new HashMap<Commit, List<EntityRef>>();
        for (int i = 0; i < commits.size(); i++) {
            Commit commit = commits.get(i);
            if (i % 3 == 0) {
                ret.put(commit, Arrays.asList(new EntityRef("defect", 6657)));
            } else if (i % 3 == 1) {
                ret.put(commit, Arrays.asList(new EntityRef("requirement", 11137)));
            } else {
                ret.put(commit, Collections.<EntityRef>emptyList());
            }
        }
        return ret;
    }

}
