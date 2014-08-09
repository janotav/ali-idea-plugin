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
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.content.AliContent;
import com.hp.alm.ali.idea.content.detail.DetailContent;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.entity.edit.EntityEditStrategy;
import com.hp.alm.ali.idea.entity.edit.LockingStrategy;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.model.parser.AuditList;
import com.hp.alm.ali.idea.ui.combo.LazyComboBoxModel;

import java.util.List;

public interface ServerStrategy extends EntityQueryProcessor {

    LazyComboBoxModel getUserModel();

    LockingStrategy getLockingStrategy();

    EntityEditStrategy getEntityEditStrategy();

    List<DetailContent> getDetailContent(Entity entity);

    /**
     * Get alias for the "in development" relation from changeset entity to given entity type.
     * @param entity target entity
     * @return unique alias (applicable to cross-filters)
     */
    String getDevelopmentAlias(String entity);

    /**
     * Filter (columns) used when previous state is not recorded.
     * @param entityType entity type
     * @return columns that are by default visible
     */
    EntityQuery getDefaultTableFilter(String entityType);

    /**
     * Fields used when previous state is not recorded.
     * @param entityType entity type
     * @return field that are by default visible
     */
    List<String> getDefaultFields(String entityType);

    /**
     * Fix metadata inconsistencies.
     * @param metadata metadata
     */
    void fixMetadata(Metadata metadata);

    List<String> getCompoundEntityTypes(String entityType);

    List<Relation> getRelationList(String entityType);

    List<AliContent> getSupportedContent();

    String getFieldAlias(String entityType, String property);

    FilterChooser getFilterChooser(String entityType, boolean multiple, boolean idSelection, boolean acceptEmpty, String value);

    /**
     * Get hierarchical path logical name for entity.
     * @param entityType entity type
     * @return hierarchical path logical name
     */
    String getHierarchicalPathProperty(String entityType);

    boolean hasSecondLevelDefectLink();

    boolean canEditAttachmentFileName();

    List<String> getDefectLinkColumns();

    String getCheckinPrefix(EntityRef entityRef);

    AuditList getEntityAudit(Entity entity);

}
