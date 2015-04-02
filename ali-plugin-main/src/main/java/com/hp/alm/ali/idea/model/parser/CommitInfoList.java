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

import com.hp.alm.ali.idea.content.devmotive.CommitInfo;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.io.InputStream;

public class CommitInfoList extends AbstractList<CommitInfo> {

    public static CommitInfoList create(InputStream is) {
        try {
            return new CommitInfoList(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private CommitInfoList(InputStream is) throws XMLStreamException {
        init(is);
    }

    @Override
    protected void onStartElement(StartElement element) throws XMLStreamException {
        String localPart = element.getName().getLocalPart();
        if("commit".equals(localPart)) {
            CommitInfo commitInfo = new CommitInfo();
            Attribute id = element.getAttributeByName(new QName(null, "id"));
            if (id != null) {
                commitInfo.setId(Integer.valueOf(id.getValue()));
            }
            add(commitInfo);
        } else if("defect".equals(localPart)) {
            Attribute id = element.getAttributeByName(new QName(null, "id"));
            if (id != null && !isEmpty()) {
                getLast().addDefect(Integer.valueOf(id.getValue()));
            }
        } else if("requirement".equals(localPart)) {
            Attribute id = element.getAttributeByName(new QName(null, "id"));
            if (id != null && !isEmpty()) {
                getLast().addRequirement(Integer.valueOf(id.getValue()));
            }
        }
    }
}
