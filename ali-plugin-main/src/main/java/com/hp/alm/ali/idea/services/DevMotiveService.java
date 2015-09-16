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

import com.hp.alm.ali.idea.cfg.WorkspaceConfiguration;
import com.hp.alm.ali.rest.client.XMLOutputterFactory;
import com.hp.alm.ali.idea.content.devmotive.Commit;
import com.hp.alm.ali.idea.content.devmotive.CommitInfo;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.parser.CommitInfoList;
import com.hp.alm.ali.idea.rest.MyResultInfo;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DevMotiveService {

    private RestService restService;
    private WorkspaceConfiguration workspaceConfiguration;

    public DevMotiveService(RestService restService, WorkspaceConfiguration workspaceConfiguration) {
        this.restService = restService;
        this.workspaceConfiguration = workspaceConfiguration;
    }

    public Map<Commit, List<EntityRef>> getRelatedEntities(List<Commit> commits) {
        HashMap<Commit, List<EntityRef>> ret = new HashMap<Commit, List<EntityRef>>();

        Integer workspaceId = workspaceConfiguration.getWorkspaceId();
        if (workspaceId == null) {
            return noResponse(ret, commits);
        }

        Element commitsElem  = new Element("commits");
        for (Commit commit: commits) {
            Element commitElem = new Element("commit");
            setAttribute(commitElem, "committer", commit.getCommitterEmail(), commit.getCommitterName());
            setAttribute(commitElem, "author", commit.getAuthorEmail(), commit.getAuthorName());
            commitElem.setAttribute("revision", commit.getRevisionString());
            commitElem.setAttribute("date", CommentField.dateTimeFormat.format(commit.getDate()));
            Element messageElem = new Element("message");
            messageElem.setText(commit.getMessage());
            commitElem.addContent(messageElem);
            commitsElem.addContent(commitElem);
        }
        String commitRequest = XMLOutputterFactory.getXMLOutputter().outputString(new Document(commitsElem));

        MyResultInfo result = new MyResultInfo();
        int code = restService.post(commitRequest, result, "workspace/{0}/ali/linked-items/commits", workspaceId);

        if (code != HttpStatus.SC_OK) {
            return noResponse(ret, commits);
        }

        Iterator<CommitInfo> commitInfoIterator = CommitInfoList.create(result.getBodyAsStream()).iterator();
        for (Commit commit: commits) {
            CommitInfo next = commitInfoIterator.next();
            LinkedList<EntityRef> list;
            if (next.getId() != null) {
                list = new LinkedList<EntityRef>();
                for (int id: next.getDefects()) {
                    list.add(new EntityRef("defect", id));
                }
                for (int id: next.getRequirements()) {
                    list.add(new EntityRef("requirement", id));
                }
            } else {
                list = null;
            }
            ret.put(commit, list);
        }

        return ret;
    }

    private void setAttribute(Element element, String attribute, String ... values) {
        for (String value: values) {
            if (!StringUtils.isEmpty(value)) {
                element.setAttribute(attribute, value);
                break;
            }
        }
    }

    private Map<Commit, List<EntityRef>> noResponse(Map<Commit, List<EntityRef>> result, List<Commit> commits) {
        for (Commit commit: commits) {
            result.put(commit, null);
        }
        return result;
    }
}
