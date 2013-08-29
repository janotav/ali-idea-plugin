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

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.EntityLabelService;

import javax.swing.JLabel;

public class EntityLabel extends JLabel {

    private String label;

    public EntityLabel(final String entityType, final EntityLabelService entityLabelService, final String suffix) {
        entityLabelService.loadEntityLabelAsync(entityType, new AbstractCachingService.DispatchCallback<String>() {
            @Override
            public void loaded(String entityLabel) {
                label = entityLabel;
                setSuffix(suffix);
            }
        });
    }

    public void setSuffix(String suffix) {
        setText(label + suffix);
    }
}
