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

import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.model.Entity;

public class AgmUrlService {

    private AliProjectConfiguration conf;

    public AgmUrlService(AliProjectConfiguration conf) {
        this.conf = conf;
    }

    public String getBacklogUrl(Entity entity, String tenantId) {
        StringBuffer buf = new StringBuffer();
        buf.append(conf.getLocation());
        buf.append("/webui/alm/");
        buf.append(conf.getDomain());
        buf.append("/");
        buf.append(conf.getProject());
        buf.append("/apm/?TENANTID=");
        buf.append(tenantId);
        buf.append("#product/backlog_items/shared.update;entityTypeName=");
        buf.append(entity.getType());
        buf.append(";entityId=");
        buf.append(entity.getId());
        return buf.toString();
    }

    public String getBuildDetailUrl(Entity entity, String tenantId) {
        StringBuffer buf = new StringBuffer();
        buf.append(conf.getLocation());
        buf.append("/webui/alm/");
        buf.append(conf.getDomain());
        buf.append("/");
        buf.append(conf.getProject());
        buf.append("/apm/?TENANTID=");
        buf.append(tenantId);
        buf.append("#builds/main/shared.buildDetails;entityId=");
        buf.append(entity.getId());
        return buf.toString();
    }

    public String getChangesetDetailUrl(Entity entity, String tenantId) {
        StringBuffer buf = new StringBuffer();
        buf.append(conf.getLocation());
        buf.append("/webui/alm/");
        buf.append(conf.getDomain());
        buf.append("/");
        buf.append(conf.getProject());
        buf.append("/apm/?TENANTID=");
        buf.append(tenantId);
        buf.append("#sourceCode/main/shared.changeSetDetail;changesetId=");
        buf.append(entity.getId());
        return buf.toString();
    }
}
