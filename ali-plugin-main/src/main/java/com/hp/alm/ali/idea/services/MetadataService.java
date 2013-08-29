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
import com.hp.alm.ali.idea.rest.RestService;
import com.intellij.openapi.project.Project;

import java.util.LinkedList;

public class MetadataService extends AbstractCachingService<String, Metadata, AbstractCachingService.Callback<Metadata>> {

    private Project project;
    private MetadataSimpleService metadataService;
    private RestService restService;

    public MetadataService(Project project, MetadataSimpleService metadataService, RestService restService) {
        super(project);
        this.project = project;
        this.metadataService = metadataService;
        this.restService = restService;
    }

    public Metadata getEntityMetadata(String entityName) {
        return getValue(entityName);
    }

    public void loadEntityMetadataAsync(String entityName, MetadataCallback callback) {
        getValueAsync(entityName, Proxy.create(callback));
    }

    @Override
    protected Metadata doGetValue(String entityName) {
        LinkedList<String> list = new LinkedList<String>();
        list.add(entityName);
        list.addAll(restService.getModelCustomization().getCompoundEntityTypes(entityName));

        Metadata result = new Metadata(project, entityName, false);

        for(String entityType: list) { // make the requests run in parallel if necessary
            metadataService.getEntityMetadataAsync(entityType, null);
        }
        for(String entityType: list) { // collect the results
            Metadata metadata = metadataService.getEntityMetadata(entityType);
            result.add(metadata);
        }

        restService.getModelCustomization().fixMetadata(result);

        return result;
    }

    public Metadata getCachedEntityMetadata(String entityType) {
        return getCachedValue(entityType);
    }

    public static class Proxy implements Callback<Metadata>, FailureCallback {

        private MetadataCallback callback;

        public static Proxy create(MetadataCallback callback) {
            if(callback instanceof DispatchMetadataCallback) {
                return new DispatchProxy(callback);
            } else {
                return new Proxy(callback);
            }
        }

        private Proxy(MetadataCallback callback) {
            this.callback = callback;
        }

        @Override
        public void loaded(Metadata data) {
            if(callback != null) {
                callback.metadataLoaded(data);
            }
        }

        @Override
        public void failed() {
            if(callback != null) {
                callback.metadataFailed();
            }
        }
    }

    public static class DispatchProxy extends Proxy implements DispatchCallback<Metadata> {

        private DispatchProxy(MetadataCallback callback) {
            super(callback);
        }
    }

    public static interface MetadataCallback {

        void metadataLoaded(Metadata metadata);

        void metadataFailed();

    }

    public static interface DispatchMetadataCallback extends MetadataCallback {

        // callback to be executed in the context of the dispatching thread

    }
}
