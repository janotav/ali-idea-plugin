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

package com.hp.alm.ali.idea.entity.table;

import com.hp.alm.ali.idea.entity.EntityFilter;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.services.MetadataService;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.filter.FilterManager;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.services.ProjectListService;
import com.hp.alm.ali.idea.filter.FilterChooser;
import com.hp.alm.ali.idea.filter.FilterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import net.coderazzi.filters.gui.CustomChoice;
import net.coderazzi.filters.gui.IFilterEditor;
import net.coderazzi.filters.gui.IFilterHeaderObserver;
import net.coderazzi.filters.gui.TableFilterHeader;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FilterObserver implements IFilterHeaderObserver {

    private ProjectListService projectListService;
    private TranslateService translateService;
    private FilterManager filterManager;
    private MetadataService metadataService;
    final private List<ChangeListener> listeners = new LinkedList<ChangeListener>();

    private Map<String, String> lastValues = new HashMap<String, String>();
    private EntityFilter query;
    private Context context;
    private List<Field> fields;

    public FilterObserver(Project project, Context context, List<Field> fields) {
        this.query = context.getEntityQuery();
        this.fields = fields;
        this.context = context;
        projectListService = project.getComponent(ProjectListService.class);
        translateService = project.getComponent(TranslateService.class);
        filterManager = project.getComponent(FilterManager.class);
        metadataService = project.getComponent(MetadataService.class);
    }

    public void addChangeListener(ChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void tableFilterEditorCreated(TableFilterHeader header, IFilterEditor editor, TableColumn tableColumn) {
        Field field = fields.get(tableColumn.getModelIndex());
        if(field == null || !field.isCanFilter()) {
            editor.setEditable(false);
            editor.setUserInteractionEnabled(false);
        } else {
            boolean chooser = false;
            FilterFactory filterFactory = filterManager.getFilterFactory(context, query.getEntityType(), field, true);
            if(filterFactory != null) {
                List<String> customChoices = filterFactory.getCustomChoices();
                if(customChoices != null) {
                    setCustomChoices(editor, customChoices);
                } else {
                    chooser = true;
                }
            } else {
                updateCustomChoices(query.getEntityType(), field, editor);
            }
            if(chooser) {
                editor.setEditable(false);
                HashSet<CustomChoice> set = new HashSet<CustomChoice>();
                set.add(AnyChoice.choose);
                editor.setCustomChoices(set);
            }
            setContent(filterFactory, query.getEntityType(), field.getName(), query.getValue(field.getName()), editor);
        }
    }

    public void tableFilterEditorExcluded(TableFilterHeader header, IFilterEditor editor, TableColumn tableColumn) {
    }

    public void tableFilterUpdated(TableFilterHeader header, final IFilterEditor editor, TableColumn tableColumn) {
        Object content = editor.getContent();
        Field field = fields.get(tableColumn.getModelIndex());
        FilterFactory factory = filterManager.getFilterFactory(context, query.getEntityType(), field, true);
        if(AnyChoice.choose.equals(content)) {
            // restore the previous editor value (we record it on every change)
            // this prevents double-popup that started to appear after instant filtering was turned off
            editor.setContent(lastValues.get(field.getName()));

            String value = query.getValue(field.getName());
            FilterChooser popup = factory.createChooser(value);
            popup.show();
            if(query.setValue(field.getName(), popup.getSelectedValue())) {
                fireChangeEvent();
            }
            setContent(factory, query.getEntityType(), field.getName(), popup.getSelectedValue(), editor);
        } else {
            if(!content.toString().isEmpty() && factory != null && factory.getCustomChoices() == null) {
                // ignore, it's the callback from resolver that resolved the filter display value
            } else {
                if(query.setValue(field.getName(), content.toString())) {
                    fireChangeEvent();
                }
            }
        }
    }

    private void fireChangeEvent() {
        synchronized (listeners) {
            for(ChangeListener listener: listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    private void setContent(FilterFactory factory, final String entityType, final String prop, final String value, final IFilterEditor editor) {
        if(value != null && !value.isEmpty() && factory != null) {
            editor.setMaxHistory(0);

            metadataService.loadEntityMetadataAsync(entityType, new MetadataService.MetadataCallback() {
                @Override
                public void metadataLoaded(Metadata metadata) {
                    Field field = metadata.getField(prop);
                    translateService.convertQueryModelToView(field, value, new ValueCallback() {
                        @Override
                        public void value(String newValue) {
                            doSetEditorContent(editor, prop, newValue);
                        }
                    });
                }

                @Override
                public void metadataFailed() {
                    doSetEditorContent(editor, prop, value); // can't translate without metadata, query may fail
                }
            });
        } else {
            doSetEditorContent(editor, prop, value);
        }
    }

    private void doSetEditorContent(final IFilterEditor editor, final String field, final String value) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                editor.setContent(value);
                lastValues.put(field, value);
            }
        });
    }

    private void updateCustomChoices(String entityType, Field field, IFilterEditor editor) {
        Integer listId = field.getListId();
        if(listId != null) {
            List<String> list = projectListService.getProjectList(entityType, field);
            setCustomChoices(editor, EntityTableModel.quote(list));
        }
    }

    private void setCustomChoices(IFilterEditor editor, List<String> values) {
        if(values != null) {
            HashSet<CustomChoice> set = new HashSet<CustomChoice>();
            for(String value: values) {
                set.add(new AnyChoice(value));
            }
            editor.setCustomChoices(set);
            editor.setEditable(false);
        }
    }
}
