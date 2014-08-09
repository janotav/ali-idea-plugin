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

import com.hp.alm.ali.idea.content.detail.AttachmentTableLoader;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.cfg.EntityFields;
import com.hp.alm.ali.idea.content.AliContent;
import com.hp.alm.ali.idea.content.defect.DefectsContent;
import com.hp.alm.ali.idea.content.requirements.RequirementsContent;
import com.hp.alm.ali.idea.content.settings.SettingsContent;
import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.content.detail.LinksTableLoader;
import com.hp.alm.ali.idea.content.detail.TableContent;
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategy;
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategyImpl;
import com.hp.alm.ali.idea.entity.edit.MayaLock;
import com.hp.alm.ali.idea.entity.edit.LockingStrategy;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.model.parser.AuditList;
import com.hp.alm.ali.idea.model.type.BuildDurationType;
import com.hp.alm.ali.idea.model.type.BuildStatusType;
import com.hp.alm.ali.idea.model.type.DefectLinkIdType;
import com.hp.alm.ali.idea.model.type.DefectLinkNameType;
import com.hp.alm.ali.idea.model.type.DefectLinkStatusType;
import com.hp.alm.ali.idea.model.type.DefectLinkTypeType;
import com.hp.alm.ali.idea.model.type.FileSizeType;
import com.hp.alm.ali.idea.model.type.PercentType;
import com.hp.alm.ali.idea.model.type.PlainTextType;
import com.hp.alm.ali.idea.model.type.RequirementTypeType;
import com.hp.alm.ali.idea.model.type.TargetReleaseCycleType;
import com.hp.alm.ali.idea.model.type.TargetReleaseType;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.chooser.EntityChooser;
import com.hp.alm.ali.idea.ui.chooser.FlatChooser;
import com.hp.alm.ali.idea.ui.chooser.HierarchicalChooser;
import com.hp.alm.ali.idea.ui.combo.LazyComboBoxModel;
import com.hp.alm.ali.idea.ui.combo.ProjectUsersComboBoxModel;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MayaStrategy implements ServerStrategy {

    private static Map<String, String> developmentAliasMap;
    static  {
        developmentAliasMap = new HashMap<String, String>();
        developmentAliasMap.put("defect", "defect");
        developmentAliasMap.put("requirement", "requirement");
        developmentAliasMap.put("build-instance", "build-instance");
    }

    private static Map<String, String> hierarchicalPath;
    static  {
        hierarchicalPath = new HashMap<String, String>();
        hierarchicalPath.put("release-folder", "path");
        hierarchicalPath.put("test-set-folder", "hierarchical-path");
        hierarchicalPath.put("favorite-folder", "path");
        hierarchicalPath.put("requirement", "hierarchical-path");
    }

    private static Map<String, EntityQuery> mayaFilter;
    static {
        mayaFilter = new HashMap<String, EntityQuery>();
        addMayaFilter(requirementFilter());
        addMayaFilter(defectFilter());
        addMayaFilter(buildFilter());
        addMayaFilter(buildTypeFilter());
        addMayaFilter(changesetFilter());
        addMayaFilter(defectLinkFilter());
    }

    private static Map<String, EntityFields> mayaFields;
    static {
        mayaFields = new HashMap<String, EntityFields>();
        mayaFields.put("requirement", requirementFields());
        mayaFields.put("defect", defectFields());
        mayaFields.put("build-instance", buildFields());
    }

    protected Project project;
    protected RestService restService;
    protected EntityService entityService;
    protected AliStrategyUtil aliStrategyUtil;
    private EntityEditStrategy entityEditStrategy;

    public MayaStrategy(Project project, RestService restService) {
        this.project = project;
        this.restService = restService;

        entityEditStrategy = project.getComponent(EntityEditStrategyImpl.class);
        entityService = project.getComponent(EntityService.class);
        aliStrategyUtil = project.getComponent(AliStrategyUtil.class);
    }

    @Override
    public EntityQuery preProcess(EntityQuery query) {
        EntityQuery clone = query.clone();

        // suppress client-side virtual fields
        LinkedHashMap<String,Integer> columns = clone.getColumns();
        boolean hasVirtual = false;
        for(Iterator<String> it = columns.keySet().iterator(); it.hasNext(); ) {
            if(it.next().startsWith("virtual:")) {
                it.remove();
                hasVirtual = true;
            }
        }
        if(hasVirtual) {
            clone.setColumns(columns);
        }

        if(!clone.getColumns().isEmpty()) {
            // when explicitly listing columns explicitly, add those implicitly required
            if("build-artifact".equals(query.getEntityType())) {
                // mandatory otherwise server fails to respond (needed for virtual field calculation)
                clone.addColumn("build", 1);
                clone.addColumn("path", 1);
            } else if("attachment".equals(query.getEntityType())) {
                // following are needed for the action context construction
                clone.addColumn("name", 1);
                clone.addColumn("parent-type", 1);
                clone.addColumn("parent-id", 1);
                clone.addColumn("file-size", 1); // this one should be queried
            } else if("defect-link".equals(query.getEntityType())) {
                // following are needed for the action context construction
                clone.addColumn("first-endpoint-id", 1);
                clone.addColumn("second-endpoint-id", 1);
                clone.addColumn("second-endpoint-type", 1);
                clone.addColumn("second-endpoint-name", 1);
                clone.addColumn("second-endpoint-status", 1);
            } else if("changeset".equals(query.getEntityType())) {
                // needed for ShowAffectedPathsAction
                clone.addColumn("rev", 1);
            } else if("changeset-file".equals(query.getEntityType())) {
                // workaround for ALM server issue: query fails unless following fields are present
                clone.addColumn("path", 1);
                clone.addColumn("revision", 1);
                clone.addColumn("operation", 1);
                clone.addColumn("file-type", 1);
            }
        }

        return clone;
    }

    @Override
    public LazyComboBoxModel getUserModel() {
        return new ProjectUsersComboBoxModel(project);
    }

    @Override
    public LockingStrategy getLockingStrategy() {
        return new MayaLock(project);
    }

    @Override
    public EntityEditStrategy getEntityEditStrategy() {
        return entityEditStrategy;
    }

    @Override
    public List<DetailContent> getDetailContent(Entity entity) {
        List<DetailContent> ret = new ArrayList<DetailContent>();
        if("requirement".equals(entity.getType())) {
            ret.add(attachmentTable(entity));
        }
        if("defect".equals(entity.getType())) {
            ret.add(attachmentTable(entity));
            ret.add(linkedTable(entity));
        }
        if("build-instance".equals(entity.getType())) {
            EntityQuery detailQuery  = new EntityQuery("build-artifact");
            detailQuery.setValue("build", String.valueOf(entity.getId()));
            ret.add(aliStrategyUtil.detailTable(entity, detailQuery, "Artifacts", IconLoader.getIcon("/nodes/artifact.png"), null));
        }
        return ret;
    }

    private TableContent attachmentTable(Entity entity) {
        EntityQuery attachmentQuery = new EntityQuery("attachment");
        final EntityRef parent = new EntityRef(entity);
        attachmentQuery.setParent(parent);
        attachmentQuery.setValue("parent-id", String.valueOf(entity.getId()));
        attachmentQuery.setValue("parent-type", entity.getType());
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.attachment", "detail", true);
        return new TableContent(project, entity, "Attachments", IconLoader.getIcon("/attachment_16.png"), actionToolbar, new AttachmentTableLoader(project, entity, attachmentQuery, attachmentQuery.getPropertyMap().keySet()));
    }

    protected TableContent linkedTable(Entity entity) {
        EntityQuery linkQuery = new EntityQuery("defect-link");
        final EntityRef parent = new EntityRef(entity);
        linkQuery.setParent(parent);
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("hpali.defect-link", "detail", true);
        return new TableContent(project, entity, "Links", IconLoader.getIcon("/actions/erDiagram.png"), actionToolbar, new LinksTableLoader(project, entity, linkQuery, linkedTableHiddenFields()));
    }

    protected Set<String> linkedTableHiddenFields() {
        HashSet<String> set = new HashSet<String>();
        set.add("second-endpoint-id");
        set.add("second-endpoint-name");
        set.add("second-endpoint-type");
        set.add("second-endpoint-status");
        set.add("first-endpoint-id");
        return set;
    }

    @Override
    public String getDevelopmentAlias(String entity) {
        return developmentAliasMap.get(entity);
    }

    @Override
    public EntityQuery getDefaultTableFilter(String entityType) {
        EntityQuery query = new EntityQuery(entityType);
        EntityQuery defaults = mayaFilter.get(entityType);
        if(defaults != null) {
            query.copyFrom(defaults);
        } else {
            query.addColumn("id", 50);
            query.addColumn("name", 100);
            query.addColumn("description", 150);
        }
        return query;
    }

    @Override
    public List<String> getDefaultFields(String entityType) {
        EntityFields defaults = mayaFields.get(entityType);
        if(defaults != null) {
            return defaults.getColumns();
        } else {
            return Arrays.asList("id", "name", "description");
        }
    }

    @Override
    public void fixMetadata(Metadata metadata) {
        fixMetadataInherited(metadata);

        // following are supported in Horizon (don't inherit them)
        if("defect-link".equals(metadata.getEntityType())) {
            metadata.getField("link-type").setCanFilter(false);
            metadata.getField("comment").setCanFilter(false);
        }
    }

    protected void fixMetadataInherited(Metadata metadata) {
        if("build-instance".equals(metadata.getEntityType())) {
            // build status is enum although it is not defined as such
            metadata.getField("status").setClazz(BuildStatusType.class);

            // present human readable value instead of non-descriptive number
            metadata.getField("duration").setClazz(BuildDurationType.class);

            // percent value
            metadata.getField("test-success").setClazz(PercentType.class);

            metadata.getField("release").setReferencedType("release");
            metadata.getField("type").setReferencedType("build-type");
        }

        if("build-artifact".equals(metadata.getEntityType())) {
            // reference to enclosing build instance
            metadata.getField("build").setReferencedType("build-instance");
        }

        if("requirement".equals(metadata.getEntityType())) {
            metadata.getField("type-id").setClazz(RequirementTypeType.class);
            metadata.getField("target-rel").setReferencedType("release");
            metadata.getField("target-rel").setClazz(TargetReleaseType.class);
            metadata.getField("target-rcyc").setReferencedType("release-cycle");
            metadata.getField("target-rcyc").setClazz(TargetReleaseCycleType.class);
        }

        if("defect".equals(metadata.getEntityType())) {
            metadata.getField("target-rel").setReferencedType("release");
            metadata.getField("target-rcyc").setReferencedType("release-cycle");
            metadata.getField("detected-in-rel").setReferencedType("release");
            metadata.getField("detected-in-rcyc").setReferencedType("release-cycle");
        }

        if("defect-link".equals(metadata.getEntityType())) {
            metadata.getField("second-endpoint-type").setCanFilter(false);

            // avoid resolving the entity ID field, there is dedicated column for it
            metadata.getField("first-endpoint-id").setReferencedType(null);

            // avoid volatile columns that change semantics based on query context
            metadata.removeField("second-endpoint-name");
            metadata.removeField("second-endpoint-status");

            // status of linked entity
            Field statusField = new Field("virtual:linked-status", "Entity Status");
            statusField.setClazz(DefectLinkStatusType.class);
            metadata.addField(statusField);

            // name of of linked entity
            Field nameField = new Field("virtual:linked-name", "Entity Name");
            nameField.setClazz(DefectLinkNameType.class);
            metadata.addField(nameField);

            // ID of of linked entity
            Field idField = new Field("virtual:linked-id", "Entity ID");
            idField.setClazz(DefectLinkIdType.class);
            metadata.addField(idField);

            // type of of linked entity
            Field typeField = new Field("virtual:linked-type", "Entity Type");
            typeField.setClazz(DefectLinkTypeType.class);
            metadata.addField(typeField);
        }

        if("attachment".equals(metadata.getEntityType())) {
            metadata.getField("description").setClazz(PlainTextType.class);
            metadata.getField("file-size").setClazz(FileSizeType.class);
        }
    }

    @Override
    public List<String> getCompoundEntityTypes(String entityType) {
        return Collections.emptyList();
    }

    @Override
    public List<Relation> getRelationList(String entityType) {
        if("defect".equals(entityType)) {
            return relationList("requirement", "test-set");
        }

        if("requirement".equals(entityType)) {
            return relationList("release");
        }

        return new ArrayList<Relation>();
    }

    @Override
    public List<AliContent> getSupportedContent() {
        return new ArrayList<AliContent>(Arrays.asList(
                DefectsContent.getInstance(),
                RequirementsContent.getInstance(),
                SettingsContent.getInstance()));
    }

    @Override
    public String getFieldAlias(String entityType, String property) {
        return property;
    }

    @Override
    public String getHierarchicalPathProperty(String entityType) {
        return hierarchicalPath.get(entityType);
    }

    @Override
    public boolean hasSecondLevelDefectLink() {
        return true;
    }

    @Override
    public boolean canEditAttachmentFileName() {
        return false;
    }

    @Override
    public List<String> getDefectLinkColumns() {
        return Arrays.asList(
                "first-endpoint-id",
                "second-endpoint-id",
                "second-endpoint-type",
                "second-endpoint-status",
                "second-endpoint-name",
                "comment");
    }

    @Override
    public String getCheckinPrefix(EntityRef entityRef) {
        return entityRef.toString() + ":";
    }

    @Override
    public AuditList getEntityAudit(Entity entity) {
        InputStream is = restService.getForStream("{0}s/{1}/audits", entity.getType(), entity.getId());
        return AuditList.create(is);
    }

    @Override
    public FilterChooser getFilterChooser(String entityType, boolean multiple, boolean idSelection, boolean acceptEmpty, String value) {
        EntityChooser dialog;
        if(Metadata.getChildEntity(entityType) != null || Metadata.getParentEntity(entityType) != null) {
            dialog = new HierarchicalChooser(project, entityType, false, multiple, idSelection, acceptEmpty, null);
        } else {
            dialog = new FlatChooser(project, entityType, multiple, acceptEmpty, null);
        }
        dialog.setValue(value);
        return dialog;
    }

    protected List<Relation> relationList(String ... entityTypes) {
        ArrayList<Relation> relations = new ArrayList<Relation>(entityTypes.length);
        for(String entityType: entityTypes) {
            relations.add(new Relation(entityType));
        }
        return relations;
    }

    private static EntityQuery requirementFilter() {
        EntityQuery filter = new EntityQuery("requirement");
        filter.addColumn("id", 25);
        filter.addColumn("name", 150);
        filter.addColumn("target-rel", 75);
        filter.addColumn("owner", 50);
        filter.addColumn("req-priority", 50);
        return filter;
    }

    private static EntityFields requirementFields() {
        EntityFields fields = new EntityFields();
        fields.addColumn("id");
        fields.addColumn("target-rel");
        fields.addColumn("owner");
        fields.addColumn("req-priority");
        fields.addColumn("name");
        fields.addColumn("req-comment");
        fields.addColumn("dev-comments");
        return fields;
    }

    private static EntityQuery buildFilter() {
        EntityQuery filter = new EntityQuery("build-instance");
        filter.addColumn("id", 50);
        filter.addColumn("type", 50);
        filter.addColumn("release", 50);
        filter.addColumn("number", 50);
        filter.addColumn("revision", 50);
        filter.addColumn("status", 50);
        filter.addColumn("start-date", 75);
        filter.addColumn("qa-status", 50);
        filter.addColumn("category", 50);
        return filter;
    }

    private static EntityFields buildFields() {
        EntityFields fields = new EntityFields();
        fields.addColumn("id");
        fields.addColumn("type");
        fields.addColumn("release");
        fields.addColumn("number");
        fields.addColumn("revision");
        fields.addColumn("status");
        fields.addColumn("start-date");
        fields.addColumn("duration");
        fields.addColumn("qa-status");
        fields.addColumn("test-success");
        fields.addColumn("test-coverage");
        fields.addColumn("category");
        fields.addColumn("build-system-url");
        return fields;
    }

    private static EntityQuery defectFilter() {
        EntityQuery filter = new EntityQuery("defect");
        filter.addColumn("id", 25);
        filter.addColumn("name", 150);
        filter.addColumn("status", 50);
        filter.addColumn("target-rel", 75);
        filter.addColumn("severity", 50);
        filter.addColumn("owner", 50);
        filter.addColumn("priority", 50);
        return filter;
    }

    private static EntityFields defectFields() {
        EntityFields fields = new EntityFields();
        fields.addColumn("id");
        fields.addColumn("name");
        fields.addColumn("severity");
        fields.addColumn("status");
        fields.addColumn("target-rel");
        fields.addColumn("owner");
        fields.addColumn("priority");
        fields.addColumn("description");
        fields.addColumn("dev-comments");
        return fields;
    }

    private static EntityQuery buildTypeFilter() {
        EntityQuery filter = new EntityQuery("build-type");
        filter.addColumn("id", 50);
        filter.addColumn("name", 50);
        filter.addColumn("release", 50);
        filter.addColumn("description", 100);
        filter.addColumn("category", 50);
        filter.addColumn("enabled", 50);
        filter.addColumn("default", 50);
        return filter;
    }

    private static EntityQuery changesetFilter() {
        EntityQuery filter = new EntityQuery("changeset");
        filter.addColumn("id", 50);
        filter.addColumn("revision", 50);
        filter.addColumn("date", 50);
        filter.addColumn("owner", 50);
        filter.addColumn("description", 100);
        return filter;
    }

    private static EntityQuery defectLinkFilter() {
        EntityQuery filter = new EntityQuery("defect-link");
        filter.addColumn("comment", 200);
        filter.addColumn("link-type", 75);
        filter.addColumn("virtual:linked-id", 55);
        filter.addColumn("virtual:linked-name", 125);
        filter.addColumn("virtual:linked-status", 55);
        filter.addColumn("virtual:linked-type", 55);
        return filter;
    }

    private static void addMayaFilter(EntityQuery entityQuery) {
        mayaFilter.put(entityQuery.getEntityType(), entityQuery);
    }
}
