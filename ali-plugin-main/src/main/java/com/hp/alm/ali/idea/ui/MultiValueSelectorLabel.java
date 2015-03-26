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

import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.filter.MultipleItemsChooserFactory;
import com.hp.alm.ali.idea.model.ItemsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import org.apache.commons.lang.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MultiValueSelectorLabel extends JPanel implements LinkListener {

    private static final String ALL_VALUES = "<all>";

    private final Project project;
    private final String title;
    private List<String> selectedItems;
    private List<String> items;

    private final LinkLabel linkLabel;
    private final List<ChangeListener> listeners;
    private int maxWidth = 100;

    public MultiValueSelectorLabel(final Project project, final String title, List<String> selectedItems, List<String> items) {
        this.project = project;
        this.title = title;
        this.selectedItems = selectedItems;
        this.items = items;

        listeners = new LinkedList<ChangeListener>();

        linkLabel = new LinkLabel(displayValue(), null);
        linkLabel.setListener(this, null);

        add(new JLabel(title + ":"));
        add(linkLabel);
    }

    public void setMaximumWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void addChangeListener(ChangeListener changeListener) {
        synchronized (listeners) {
            listeners.add(changeListener);
        }
    }

    public List<String> getSelectedValues() {
        return Collections.unmodifiableList(selectedItems);
    }

    @Override
    public void linkSelected(LinkLabel aSource, Object aLinkData) {
        String values = StringUtils.join(selectedItems, ";");
        FilterChooser chooser = new MultipleItemsChooserFactory(project, title, true, new ItemsProvider.Loader<ComboItem>() {
            @Override
            public List<ComboItem> load() {
                return FilterManager.asItems(items, true, true);
            }
        }).createChooser(values);
        chooser.show();
        String selectedValue = chooser.getSelectedValue();
        if (selectedValue != null) {
            if (selectedValue.isEmpty()) {
                selectedItems = items;
            } else {
                selectedItems = Arrays.asList(selectedValue.split(";"));
            }
            linkLabel.setText(displayValue());
            fireChangeEvent(this);
        }
    }

    private void fireChangeEvent(Object source) {
        synchronized (listeners) {
            for(ChangeListener listener: listeners) {
                listener.stateChanged(new ChangeEvent(source));
            }
        }
    }

    private String displayValue() {
        if (items.size() == selectedItems.size()) {
            return ALL_VALUES;
        } else {
            String value = StringUtils.join(selectedItems, ";");
            if (value.length() > maxWidth) {
                return value.substring(0, maxWidth) + "...";
            } else {
                return value;
            }
        }
    }
}
