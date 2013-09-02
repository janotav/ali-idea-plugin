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

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.ui.editor.field.CommentField;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.protocol.td.Handler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskType;

import javax.swing.Icon;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HpAlmTask extends Task {
    private static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private static final Map<String, String> interfaceMap;
    static {
        interfaceMap = new HashMap<String, String>();
        interfaceMap.put("defect", "IBug");
        interfaceMap.put("requirement", "IRequirement");
    }

    private static boolean openInBrowserAvailable = false;

    static {
        if(SystemInfo.isWindows && System.getProperty("ali.no.td.handler") == null) {
            try {
                URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                    public URLStreamHandler createURLStreamHandler(String proto) {
                        if("td".equals(proto)) {
                            return new Handler();
                        } else {
                            return null;
                        }
                    }
                });
                openInBrowserAvailable = true;
            } catch(Error e) {
                // cannot open browser links
            }
        }
    }

    private Project project;
    private Entity entity;

    public HpAlmTask(Project project, Entity entity) {
        this.project = project;
        this.entity = entity;
    }

    public boolean isInitialized() {
        return entity.isComplete() ||
                (entity.isInitialized("name") &&
                 hasDateProperties(entity) &&
                 entity.isInitialized(getDescriptionField(entity.getType())) &&
                 (entity.isInitialized("dev-comments") || entity.isInitialized("comments"))); // comments used for requirement in ALM 12
    }

    public String getId() {
        return new EntityRef(entity).toString();
    }

    public String getSummary() {
        return entity.getPropertyValue("name");
    }

    public String getDescription() {
        return HTMLAreaField.toPlainText(entity.getPropertyValue(getDescriptionField(entity.getType())), false);
    }

    public Comment[] getComments() {
        return parseComments(entity.getPropertyValue(entity.isInitialized("dev-comments")? "dev-comments": "comments"));
    }

    private Comment[] parseComments(String commentBlob) {
        List<Comment> list = new LinkedList<Comment>();
        int last = 0;
        Pattern separator = Pattern.compile("<(b|strong)>________________________________________</(b|strong)>");
        Matcher matcher = separator.matcher(commentBlob);
        while(matcher.find(last)) {
            list.add(HpAlmComment.parse(commentBlob.substring(last, matcher.start())));
            last = matcher.end();
        }
        if(last < commentBlob.length()) {
            list.add(HpAlmComment.parse(commentBlob.substring(last)));
        }
        return list.toArray(new Comment[list.size()]);
    }

    public Icon getIcon() {
        return null;
    }

    public TaskType getType() {
        if("defect".equals(entity.getType())) {
            return TaskType.BUG;
        } else {
            return TaskType.FEATURE;
        }
    }

    public Date getUpdated() {
        return parseDate(entity.getPropertyValue("last-modified"));
    }

    public Date getCreated() {
        try {
            Date date = CommentField.dateFormat.parse(entity.getPropertyValue("creation-time"));
            if("requirement".equals(entity.getType())) {
                return new Date(date.getTime() + timeFormat.parse(entity.getPropertyValue("req-time")).getTime() + TimeZone.getDefault().getRawOffset());
            }
            return date;
        } catch(ParseException e) {
            return getUpdated();
        }
    }

    private boolean hasDateProperties(Entity entity) {
        if(!entity.isInitialized("last-modified") || !entity.isInitialized("creation-time")) {
            return false;
        }

        if("requirement".equals(entity.getType()) && !entity.isInitialized("req-time")) {
            return false;
        }

        return true;
    }

    public boolean isClosed() {
        if("defect".equals(entity.getType())) {
            return "Closed".equals(entity.getPropertyValue("status")); // TODO: should be configurable
        } else {
            return false;
        }
    }

    // required in 11.0
    public String getCustomIcon() {
        return null;
    }

    public boolean isIssue() {
        return getType() == TaskType.BUG;
    }

    public String getIssueUrl() {
        if(openInBrowserAvailable) {
            return _getIssueUrl();
        } else {
            return null;
        }
    }

    String _getIssueUrl() {
        AliProjectConfiguration conf = project.getComponent(AliProjectConfiguration.class);
        StringBuffer url = new StringBuffer();
        url.append("td://");
        url.append(conf.getProject());
        url.append(".");
        url.append(conf.getDomain());
        url.append(".");
        url.append(conf.getLocation().replaceFirst("^[Hh][Tt][Tt][Pp][Ss]?://", ""));
        url.append("/[AnyModule]?EntityType=");
        url.append(interfaceMap.get(entity.getType()));
        url.append("&EntityID=");
        url.append(entity.getId());
        url.append("&ShowDetails=Y");
        return url.toString();
    }

    public static Date parseDate(String dateStr) {
        try {
            return dateTimeFormat.parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }

    }

    public static String getDescriptionField(String entityType) {
        if("defect".equals(entityType)) {
            return "description";
        } else {
            return "req-comment";
        }
    }
}
