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

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.content.AliContent;
import com.hp.alm.ali.idea.content.backlog.BacklogContent;
import com.hp.alm.ali.idea.content.builds.BuildsContent;
import com.hp.alm.ali.idea.content.defect.DefectsContent;
import com.hp.alm.ali.idea.content.settings.SettingsContent;
import com.hp.alm.ali.idea.content.taskboard.TaskBoardContent;
import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.content.detail.TableContent;
import com.hp.alm.ali.idea.content.detail.TaskTableLoader;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.edit.DummyLock;
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategy;
import com.hp.alm.ali.idea.entity.edit.HorizonEditStrategy;
import com.hp.alm.ali.idea.entity.edit.LockingStrategy;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.model.parser.AuditList;
import com.hp.alm.ali.idea.model.type.BacklogBlockedType;
import com.hp.alm.ali.idea.model.type.BacklogStatusDefectType;
import com.hp.alm.ali.idea.model.type.BacklogEntityType;
import com.hp.alm.ali.idea.model.type.DefectStatusType;
import com.hp.alm.ali.idea.model.type.FeatureType;
import com.hp.alm.ali.idea.model.type.ReleaseType;
import com.hp.alm.ali.idea.model.type.SprintType;
import com.hp.alm.ali.idea.model.type.TeamType;
import com.hp.alm.ali.idea.model.type.ThemeType;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.ui.chooser.FlatChooser;
import com.hp.alm.ali.idea.ui.combo.LazyComboBoxModel;
import com.hp.alm.ali.idea.ui.combo.TeamMembersComboBoxModel;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import javax.swing.SortOrder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HorizonStrategy extends ApolloStrategy {

    private static Set<String> INVISIBLE_FIELDS = new HashSet<String>(Arrays.asList(
            "release-backlog-item.id",
            "release-backlog-item.ver-stamp",
            "release-backlog-item.vts",
            "release-backlog-item.is-leaf",
            "release-backlog-item.aggr-assignee",
            "release-backlog-item.bi-req-type",

            "requirement.target-rel-varchar",
            "requirement.target-rcyc-varchar",
            "requirement.req-ver-stamp",
            "requirement.check-out-user-name",
            "requirement.failed-at",
            "requirement.passed-at",
            "requirement.not-run-at",
            "requirement.has-linkage",
            "requirement.has-rich-content",
            "requirement.istemplate",
            "requirement.req-type",
            "requirement.type-id",
            "requirement.parent-id",
            "requirement.order-id",
            "requirement.father-name",
            "requirement.hierarchical-path",
            "requirement.req-review",
            "requirement.req-rich-content",
            "requirement.target-rcyc", // duplicates release-backlog-item.sprint-id
            "requirement.target-rel", // duplicates release-backlog-item.release-id
            "requirement.product-id",
            "requirement.application-name",
            "requirement.theme-products",
            // request-* fields excluded by code
            // rbt-* fields excluded by code
            // vc-* fields excluded by code

            "defect.target-rel", // duplicates release-backlog-item.release-id
            "defect.target-rcyc", // duplicates release-backlog-item.sprint-id
            "defect.project",
            "defect.run-reference",
            "defect.step-reference",
            "defect.cycle-id",
            "defect.test-reference",
            "defect.cycle-reference",
            "defect.to-mail",
            "defect.bug-ver-stamp",
            "defect.owner"));

    private static Map<String, String> FIELD_ALIAS;
    static {
        FIELD_ALIAS = new HashMap<String, String>();
        FIELD_ALIAS.put("defect.owner", "release-backlog-item.owner");
        FIELD_ALIAS.put("requirement.owner", "release-backlog-item.owner");
    }

    private static Map<String, EntityQuery> horizonFilter;
    static {
        horizonFilter = new HashMap<String, EntityQuery>();
        addHorizonFilter(backlogItemFilter());
        addHorizonFilter(horizonDefectFilter());
        addHorizonFilter(teamFilter());
        addHorizonFilter(taskFilter());
    }

    private static Map<String, List<String>> horizonFields;
    static {
        horizonFields = new HashMap<String, List<String>>();
        horizonFields.put("team", teamFields());
        horizonFields.put("requirement", horizonRequirementFields());
    }

    private static Map<String, String> orderMap;
    static {
        orderMap = new HashMap<String, String>();
        orderMap.put("release-backlog-item.release-id", "target-rel");
        orderMap.put("release-backlog-item.sprint-id", "target-rcyc");
    }

    public HorizonStrategy(Project project, RestService restService) {
        super(project, restService);
    }

    @Override
    public EntityQuery preProcess(EntityQuery query) {
        EntityQuery clone = super.preProcess(query);
        if(!clone.getColumns().isEmpty()) {
            if("release-backlog-item".equals(query.getEntityType())) {
                clone.addColumn("entity-type", 1);
                clone.addColumn("entity-id", 1);
                clone.addColumn("blocked", 1);
            } else if("defect".equals(query.getEntityType()) || "requirement".equals(query.getEntityType())) {
                clone.addColumn("release-backlog-item.id", 1);
                clone.addColumn("release-backlog-item.blocked", 1);

                LinkedHashMap<String,SortOrder> order = clone.getOrder();
                if(containsAny(order.keySet(), orderMap.keySet())) {
                    remapOrder(order, orderMap);
                    clone.setOrder(order);
                }
            } else if("project-task".equals(query.getEntityType())) {
                clone.addColumn("release-backlog-item-id", 1);
            }
        }
        return clone;
    }

    private boolean containsAny(Set<String> set, Collection<String> items) {
        for(String item: items) {
            if(set.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private void remapOrder(LinkedHashMap<String,SortOrder> order, Map<String, String> orderMap) {
        ArrayList<String> list = new ArrayList<String>(order.keySet());
        for(String key: list) {
            SortOrder value = order.remove(key);
            String newKey = orderMap.get(key);
            if(newKey != null) {
                order.put(newKey, value);
            } else {
                order.put(key, value);
            }
        }
    }

    @Override
    public LazyComboBoxModel getUserModel() {
        return new TeamMembersComboBoxModel(project);
    }

    @Override
    public LockingStrategy getLockingStrategy() {
        return new DummyLock(project);
    }

    @Override
    public EntityEditStrategy getEntityEditStrategy() {
        return project.getComponent(HorizonEditStrategy.class);
    }

    @Override
    public List<DetailContent> getDetailContent(Entity entity) {
        List<DetailContent> ret = super.getDetailContent(entity);
        aliStrategyUtil.addCodeChangesDetail(this, entity, ret);
        if("defect".equals(entity.getType())) {
            ret.add(taskTable(entity));
        }
        if("requirement".equals(entity.getType())) {
            ret.add(taskTable(entity));
        }
        return ret;
    }

    private DetailContent taskTable(Entity entity) {
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.project-task", "detail", true);
        return new TableContent(project, entity, "Tasks", IconLoader.getIcon("/fileTypes/text.png"), actionToolbar, new TaskTableLoader(project, entity));
    }

    @Override
    public EntityQuery getDefaultTableFilter(String entityType) {
        EntityQuery defaults = horizonFilter.get(entityType);
        if(defaults != null) {
            EntityQuery query = new EntityQuery(entityType);
            query.copyFrom(defaults);
            return query;
        } else {
            return super.getDefaultTableFilter(entityType);
        }
    }

    @Override
    public List<String> getDefaultFields(String entityType) {
        List<String> defaults = horizonFields.get(entityType);
        if(defaults != null) {
            return defaults;
        } else {
            return super.getDefaultFields(entityType);
        }
    }

    @Override
    public void fixMetadata(Metadata metadata) {
        super.fixMetadataInherited(metadata);

        if("release-backlog-item".equals(metadata.getEntityType())) {
            // theme and feature reference requirement entity
            metadata.getField("theme-id").setReferencedType("requirement");
            metadata.getField("theme-id").setClazz(ThemeType.class);
            metadata.getField("feature-id").setReferencedType("requirement");
            metadata.getField("feature-id").setClazz(FeatureType.class);

            // custom enum-like handling
            metadata.getField("entity-type").setClazz(BacklogEntityType.class);
            metadata.getField("blocked").setClazz(BacklogBlockedType.class);

            // override labels
            metadata.getField("entity-id").setLabel("ID");
            metadata.getField("entity-name").setLabel("Name");
            metadata.getField("entity-type").setLabel("Type");
            metadata.getField("defect.priority").setLabel("Defect Priority");
            metadata.getField("requirement.req-priority").setLabel("Story Priority");
        }

        if("project-task".equals(metadata.getEntityType())) {
            // avoid "no value" option in filter selector
            metadata.getField("status").setRequired(true);
        }

        if("defect".equals(metadata.getEntityType()) || "requirement".equals(metadata.getEntityType())) {
            metadata.getField("release-backlog-item.theme-id").setReferencedType("requirement");
            metadata.getField("release-backlog-item.theme-id").setClazz(ThemeType.class);
            metadata.getField("release-backlog-item.theme-id").setEditable(false); // must be set through feature
            metadata.getField("release-backlog-item.feature-id").setReferencedType("requirement");
            metadata.getField("release-backlog-item.feature-id").setClazz(FeatureType.class);
            metadata.getField("release-backlog-item.sprint-id").setReferencedType("release-cycle");
            metadata.getField("release-backlog-item.sprint-id").setClazz(SprintType.class);
            metadata.getField("release-backlog-item.release-id").setReferencedType("release");
            metadata.getField("release-backlog-item.release-id").setClazz(ReleaseType.class);
            metadata.getField("release-backlog-item.team-id").setReferencedType("team");
            metadata.getField("release-backlog-item.team-id").setClazz(TeamType.class);
            metadata.getField("release-backlog-item.team-id").setNoSort(true);
        }

        if("defect".equals(metadata.getEntityType())) {
            metadata.getField("status").setClazz(DefectStatusType.class);
            metadata.getField("release-backlog-item.status").setClazz(BacklogStatusDefectType.class);
            metadata.getField("release-backlog-item.blocked").setClazz(BacklogBlockedType.class);
        }

        setClazz(metadata, "sprint-id", SprintType.class);
        setClazz(metadata, "team-id", TeamType.class);

        // remove fields that should be obsolete in AgM
        for(Field field: new LinkedList<Field>(metadata.getAllFields().values())) {
            String name = field.getName();

            if(isInvisible(metadata.getEntityType(), name)) {
                metadata.removeField(name);
                continue;
            }

            String relatedType = field.getRelatedType();
            if(relatedType != null) {
                String shortName = name.substring(relatedType.length() + 1);
                if(isInvisible(relatedType, shortName)) {
                    metadata.removeField(name);
                    continue;
                }
            }
        }
    }

    @Override
    public String getFieldAlias(String entityType, String property) {
        String alias = FIELD_ALIAS.get(entityType + "." + property);
        if(alias != null) {
            return alias;
        } else {
            return property;
        }
    }

    @Override
    public List<AliContent> getSupportedContent() {
        return Arrays.asList(
                BacklogContent.getInstance(),
                TaskBoardContent.getInstance(),
                DefectsContent.getInstance(),
                BuildsContent.getInstance(),
                SettingsContent.getInstance());
    }

    @Override
    public boolean canEditAttachmentFileName() {
        return false;
    }

    private boolean isInvisible(String entityType, String fieldName) {
        if(fieldName.startsWith("rbt-") || fieldName.startsWith("vc-") || fieldName.startsWith("request-")) {
            return true;
        }

        return INVISIBLE_FIELDS.contains(entityType + "." + fieldName) || INVISIBLE_FIELDS.contains("*." + fieldName);
    }

    @Override
    public List<String> getCompoundEntityTypes(String entityType) {
        if("release-backlog-item".equals(entityType)) {
            return Arrays.asList("requirement", "defect");
        } else if("requirement".equals(entityType) || "defect".equals(entityType)) {
            return Arrays.asList("release-backlog-item");
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public FilterChooser getFilterChooser(String entityType, boolean multiple, boolean idSelection, boolean acceptEmpty, String value) {
        if("requirement".equals(entityType)) {
            FlatChooser popup = new FlatChooser(project, "requirement", multiple, acceptEmpty, new EntityQueryProcessor() {
                @Override
                public EntityQuery preProcess(EntityQuery query) {
                    EntityQuery clone = query.clone();
                    clone.setValue("type-id", String.valueOf(70));
                    clone.setPropertyResolved("type-id", true);
                    return clone;
                }
            });
            popup.setValue(value);
            return popup;
        } else {
            return super.getFilterChooser(entityType, multiple, idSelection, acceptEmpty, value);
        }
    }

    @Override
    public String getCheckinPrefix(EntityRef entityRef) {
        if ("requirement".equals(entityRef.type)) {
            // we can't use entity label because it starts with capital letter
            return "user story #" + entityRef.id + ":";
        } else {
            return super.getCheckinPrefix(entityRef);
        }
    }

    @Override
    public AuditList getEntityAudit(Entity entity) {
        InputStream is = restService.getForStream("{0}s/{1}/audits", entity.getType(), entity.getId());
        AuditList auditList = AuditList.create(is);

        String bliId = (String)entity.getProperty("release-backlog-item.id");
        if (bliId != null) {
            InputStream blis = restService.getForStream("release-backlog-items/{0}/audits", bliId);
            mergeAuditLists(auditList, AuditList.create(blis));
        }
        return auditList;
    }

    private void mergeAuditLists(AuditList target, AuditList source) {
        target.addAll(source);
        Collections.sort(target, new Comparator<Audit>() {
            @Override
            public int compare(Audit audit1, Audit audit2) {
                long time2 = audit2.getDate().getTime();
                long time1 = audit1.getDate().getTime();
                if (time2 > time1) {
                    return 1;
                } else if (time2 < time1) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        Audit current = null;
        for (Iterator<Audit> it = target.iterator(); it.hasNext(); ) {
            Audit next = it.next();
            if (current != null && current.getUsername().equals(next.getUsername()) && current.getDate().equals(next.getDate())) {
                for(String[] properties: next.getProperties()) {
                    current.addProperty(properties[0], properties[1], properties[2]);
                }
                it.remove();
            } else {
                current = next;
            }
        }
    }

    private void setClazz(Metadata metadata, String fieldName, Class clazz) {
        Field field = metadata.getField(fieldName);
        if(field != null) {
            field.setClazz(clazz);
        }
    }

    private static EntityQuery horizonDefectFilter() {
        EntityQuery filter = new EntityQuery("defect");
        filter.addColumn("id", 97);
        filter.addColumn("release-backlog-item.blocked", 56);
        filter.addColumn("name", 257);
        filter.addColumn("severity", 118);
        filter.addColumn("status", 128);
        filter.addColumn("release-backlog-item.status", 99);
        filter.addColumn("release-backlog-item.owner", 145);
        filter.addColumn("release-backlog-item.team-id", 142);
        filter.addColumn("release-backlog-item.feature-id", 136);
        filter.addColumn("release-backlog-item.theme-id", 141);
        filter.addColumn("detected-by", 110);
        filter.addColumn("priority", 123);
        filter.addColumn("release-backlog-item.release-id", 155);
        filter.addColumn("release-backlog-item.sprint-id", 87);
        return filter;
    }

    private static EntityQuery backlogItemFilter() {
        EntityQuery filter = new EntityQuery("release-backlog-item");
        filter.addColumn("entity-type", 146);
        filter.addColumn("blocked", 58);
        filter.addColumn("entity-name", 898);
        filter.addColumn("status", 84);
        filter.addColumn("story-points", 75);
        filter.addColumn("owner", 119);
        filter.addColumn("team-id", 96);
        filter.addColumn("feature-id", 159);
        filter.addColumn("theme-id", 159);
        return filter;
    }

    private static EntityQuery teamFilter() {
        EntityQuery filter = new EntityQuery("team");
        filter.addColumn("id", 50);
        filter.addColumn("name", 150);
        filter.addColumn("description", 200);
        filter.addColumn("release-id", 50);
        return filter;
    }

    private static List<String> teamFields() {
        LinkedList<String> fields = new LinkedList<String>();
        fields.add("name");
        fields.add("description");
        fields.add("release-id");
        return fields;
    }

    private static List<String> horizonRequirementFields() {
        LinkedList<String> fields = new LinkedList<String>();
        fields.add("id");
        fields.add("name");
        fields.add("release-backlog-item.release-id");
        fields.add("release-backlog-item.team-id");
        fields.add("release-backlog-item.sprint-id");
        fields.add("release-backlog-item.owner");
        fields.add("req-priority");
        fields.add("description");
        fields.add("comments");
        return fields;
    }

    private static EntityQuery taskFilter() {
        EntityQuery filter = new EntityQuery("project-task");
        filter.addColumn("description", 310);
        filter.addColumn("status", 99);
        filter.addColumn("assigned-to", 105);
        filter.addColumn("invested", 68);
        filter.addColumn("remaining", 68);
        return filter;
    }

    private static void addHorizonFilter(EntityQuery entityQuery) {
        horizonFilter.put(entityQuery.getEntityType(), entityQuery);
    }
}