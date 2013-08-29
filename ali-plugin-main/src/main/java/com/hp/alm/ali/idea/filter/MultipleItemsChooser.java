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

package com.hp.alm.ali.idea.filter;

import com.hp.alm.ali.idea.model.ItemsProvider;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.ui.ComboItem;
import com.hp.alm.ali.idea.ui.MultipleItemsDialog;
import com.hp.alm.ali.idea.ui.MultipleItemsDialogModel;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;

public class MultipleItemsChooser implements FilterChooser {
    private Project project;
    private String title;
    private boolean multiple;
    private ItemsProvider<ComboItem> items;
    private Translator translator;
    private String value;

    public MultipleItemsChooser(Project project, String title, boolean multiple, ItemsProvider<ComboItem> items, Translator translator, String value) {
        this.project = project;
        this.title = title;
        this.multiple = multiple;
        this.items = items;
        this.translator = translator;
        this.value = value;
    }

    @Override
    public void show() {
        ArrayList<Object> selected = new ArrayList<Object>();
        if(value != null) {
            Collections.addAll(selected, value.split(";"));
        }
        MultipleItemsDialog<Object, ComboItem> chooser = new MultipleItemsDialog<Object, ComboItem>(project, "Select " + title, new MultipleItemsDialogModel<Object, ComboItem>(title, multiple, items, selected, translator));
        chooser.setVisible(true);
        if(chooser.isOk()) {
            value = StringUtils.join(selected.toArray(), ";");
        }
    }

    @Override
    public String getSelectedValue() {
        return value;
    }
}
