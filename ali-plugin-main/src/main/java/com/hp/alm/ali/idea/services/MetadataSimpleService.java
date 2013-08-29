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

import com.hp.alm.ali.idea.model.Metadata;
import com.intellij.openapi.project.Project;

/**
 * Do not use this service directly. It does not include compound fields. Use {@link MetadataService} instead.
 */
public class MetadataSimpleService extends AbstractCachingService<String, Metadata, AbstractCachingService.Callback<Metadata>> {

    private Project project;

    public MetadataSimpleService(Project project) {
        super(project);
        this.project = project;
    }

    @Override
    protected Metadata doGetValue(String entityType) {
        return new Metadata(project, entityType, true);
    }

    void getEntityMetadataAsync(String entityType, MetadataService.MetadataCallback callback) {
        getValueAsync(entityType, MetadataService.Proxy.create(callback));
    }

    Metadata getEntityMetadata(String entityType) {
        return getValue(entityType);
    }
}
