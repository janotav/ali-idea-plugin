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

import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.entity.EntityQueryProvider;
import com.hp.alm.ali.idea.entity.EntityRenderer;
import com.hp.alm.ali.idea.model.ItemRenderer;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.model.Entity;

public class EntityChooser extends ComboItem implements ChoosingItem {

    private EntityService entityService;

    public EntityChooser(EntityService entityService, String entityType, String description) {
        super(entityType, description);
        this.entityService = entityService;
    }

    @Override
    public ItemsProvider getItemsProvider() {
        return new EntityQueryProvider(entityService, (String)getKey());
    }

    @Override
    public ItemRenderer getItemRenderer() {
        return new EntityRenderer();
    }

    @Override
    public Object create(Object selected) {
        Entity entity = (Entity) selected;
        return new ComboItem(entity, entity.getPropertyValue("name"));
    }
}
