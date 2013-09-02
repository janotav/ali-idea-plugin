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

package com.hp.alm.ali.idea.ui.entity.table;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityQueryProcessor;
import com.hp.alm.ali.idea.entity.table.ColumnModelProxy;
import com.hp.alm.ali.idea.entity.table.EntityTableModel;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.action.ActionUtil;
import com.hp.alm.ali.idea.entity.table.DummyParserModel;
import com.hp.alm.ali.idea.entity.table.FilterObserver;
import com.hp.alm.ali.idea.entity.table.MyRowSorter;
import com.hp.alm.ali.idea.entity.table.RowSorterProxy;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.translate.NavigatingTranslator;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.model.type.ContextAware;
import com.hp.alm.ali.idea.services.FavoritesService;
import com.hp.alm.ali.idea.ui.entity.EntityStatusPanel;
import com.hp.alm.ali.idea.ui.event.PopupAdapter;
import com.hp.alm.ali.idea.ui.entity.query.EntityQueryPanel;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import org.jetbrains.annotations.NonNls;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EntityTable extends JPanel implements DataProvider {
    
    private JBTable table;
    private EntityTableModel model;
    private TranslateService translateService;
    private EntityQueryPanel entityQueryPanel;
    private EntityStatusPanel statusPanel;
    private Entity masterEntity;
    private DataProvider dataProvider;

    public EntityTable(Project project, String entityType, EntityQuery filter) {
        this(project, entityType, true, filter, Collections.<String>emptySet(), null, true, null);
    }

    public EntityTable(final Project project, final String entityType, boolean autoload, EntityQuery filter, final Set<String> hiddenFields, EntityQueryProcessor processor, final boolean useFavorites, final Entity masterEntity) {
        super(new BorderLayout());

        this.masterEntity = masterEntity;
        translateService = project.getComponent(TranslateService.class);

        table = new JBTable();
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateColumnsFromModel(false);
        table.setAutoCreateRowSorter(false);
        final MyRowSorter rowSorter = new MyRowSorter();
        table.setRowSorter(rowSorter);
        model = new EntityTableModel(project, table.getColumnModel(), autoload, entityType, filter, rowSorter, hiddenFields, processor);
        statusPanel = new EntityStatusPanel(project);
        model.setStatusIndicator(statusPanel);
        Disposer.register(project, model); // TODO: validate
        // setting table model clears the sorter, ignore this event not to lose visual indication
        rowSorter.setIgnore(true);
        table.setModel(model);
        rowSorter.setIgnore(false);
        final TableFilterHeader header = new TableFilterHeader(table, new DummyParserModel());
        /**
         * turn instant filtering off, because:
         *  a) it works nice on auto-completion field (e.g. assigned to) when item is auto-completed (whole item goes to the filter)
         *  b) it doesn't work if field has auto-completion but multiple choices still exist (only value entered so far is sent to the filter)
         *  c) it doesn't work for non-completed fields (e.g. defect summary)
         *
         * to make this work would require some kind of voodoo with the trailing wildcard that would however either changed the semantics
         * of the filter or brought corner-cases when switching from instant to final filter. moreover instant filtering would
         * most likely require some kind of delaying mechanism (configurable?) to prevent search on the very first character entered.
         * last but not least, instant filtering puts more stress on the server...
         */
        header.setInstantFiltering(false);
        FilterObserver observer = new FilterObserver(project, new Context(model.getFilter()), model.getFields());
        header.addHeaderObserver(observer);
        observer.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                model.fireFilterChangedEvent();
                model.reload();
            }
        });
        header.setAutoChoices(AutoChoices.DISABLED);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                /**
                 * FilterObserver callbacks are (sometimes) not invoked when columns are added into the model during initial load (why?).
                 * Listen on the table-structure-changed event and force the header reload.
                 */
                if(e.getFirstRow() == TableModelEvent.HEADER_ROW &&
                        e.getLastRow() == TableModelEvent.HEADER_ROW &&
                        e.getColumn() == TableModelEvent.ALL_COLUMNS &&
                        e.getType() == TableModelEvent.UPDATE) {
                    header.setTable(null);
                    header.setTable(table);
                    model.removeTableModelListener(this);
                }
            }
        });
        header.setTable(null);
        header.setTable(table);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                /**
                 * Auto-scroll to inserted values.
                 */
                if(e.getType() == TableModelEvent.INSERT) {
                    // make sure that table processes the event first
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            int viewRow = table.convertRowIndexToView(e.getFirstRow());
                            scrollTo(viewRow);
                        }
                    });
                }
            }
        });

        /**
         * turn adaptive choices off to avoid following exception (appears from time to time):
         *
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at java.util.ArrayList.RangeCheck(ArrayList.java:547)
	at java.util.ArrayList.get(ArrayList.java:322)
	at net.coderazzi.filters.gui.AdaptiveChoicesHandler$AdaptiveChoicesSupport.include(AdaptiveChoicesHandler.java:491)
	at javax.swing.DefaultRowSorter.include(DefaultRowSorter.java:914)
         */
        header.setAdaptiveChoices(false);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if(mouseEvent.isPopupTrigger()) {
                    return;
                }
                int col = table.convertColumnIndexToModel(table.getTableHeader().columnAtPoint(mouseEvent.getPoint()));
                if(model.getFields().get(col).isCanFilter()) {
                    if((mouseEvent.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
                        // append sort
                        rowSorter.toggleSortOrder(col, true);
                    } else {
                        // set sort
                        rowSorter.toggleSortOrder(col, false);
                    }
                }
            }
        });

        table.addMouseListener(new PopupAdapter() {
            public void onPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if(row < 0) {
                    return;
                }
                ActionPopupMenu popup = ActionUtil.createEntityActionPopup("table");
                popup.getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
        });
        final RowSorterProxy sorterProxy = new RowSorterProxy(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getColumnModel().addColumnModelListener(new ColumnModelProxy(model));
        table.getRowSorter().addRowSorterListener(sorterProxy);
        table.getTableHeader().addMouseListener(new PopupAdapter() {
            public void onPopup(MouseEvent e) {
                // workaround for http://bugs.sun.com/view_bug.do?bug_id=6586009
                table.getTableHeader().setDraggedColumn(null);
                ColumnHeaderPopup popup = new ColumnHeaderPopup(project, table, model, useFavorites);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        final JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                NavigatingTranslator translator = getNavigationTranslator(e);
                if(translator != null) {
                    int row = table.rowAtPoint(e.getPoint());
                    Entity entity = model.getEntity(table.convertRowIndexToModel(row));
                    HyperlinkListener listener = translator.getHyperlinkListener(entity);
                    if(listener != null) {
                        int col = table.columnAtPoint(e.getPoint());
                        Rectangle cellRect = table.getCellRect(row, col, true);
                        String value = entity.getPropertyValue(model.getFields().get(table.convertColumnIndexToModel(col)).getName());
                        Component component = table.getCellRenderer(row, col).getTableCellRendererComponent(table, value, false, false, row, col);
                        Dimension dim = component.getPreferredSize();
                        if(dim.width < 20) {
                            // make it easier to click on very narrow values (e.g. 1)
                            dim.width = 20;
                        }
                        if(e.getPoint().x + dim.width > cellRect.x + cellRect.width) { // assume right alignment
                            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            // due to JBScrollPane bug/feature the cursor needs to be specified on the encapsulating pane too
                            scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }
                // seems not necessary if JBScrollPane is used
                table.setCursor(Cursor.getDefaultCursor());
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!table.getCursor().equals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))) {
                    return;
                }
                NavigatingTranslator translator = getNavigationTranslator(e);
                if(translator != null) {
                    int row = table.rowAtPoint(e.getPoint());
                    HyperlinkListener listener = translator.getHyperlinkListener(model.getEntity(table.convertRowIndexToModel(row)));
                    if(listener != null) {
                        listener.hyperlinkUpdate(new HyperlinkEvent(table, HyperlinkEvent.EventType.ACTIVATED, null));
                    }
                }
            }
        });
        table.setDefaultRenderer(Translator.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
                int col = table.convertColumnIndexToModel(column);
                Field field = model.getFields().get(col);
                final int modelRow = table.convertRowIndexToModel(row);
                Translator translator = model.getTranslator(field, modelRow, masterEntity);
                if(translator instanceof ContextAware || (value != null && !value.toString().isEmpty())) {
                    value = translateService.translateAsync(translator, value == null? "": value.toString(), false, new ValueCallback() {
                        @Override
                        public void value(String value) {
                            model.fireTableRowsUpdated(modelRow, modelRow);
                        }
                    });
                }
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                return this;
            }
        });

        // com.intellij.ide.actions.RefreshAction not provided in 10.5.x
        ShortcutSet shortcutSet = ActionManager.getInstance().getAction(IdeActions.ACTION_REFRESH).getShortcutSet();
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                model.reload();
            }
        }.registerCustomShortcutSet(shortcutSet, table);

        entityQueryPanel = new EntityQueryPanel(project, model, entityType, hiddenFields, null, false, true, true, true);

        setBackground(Color.WHITE);
    }

    private NavigatingTranslator getNavigationTranslator(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if (row >= 0 && col >= 0) {
            Field field = model.getFields().get(table.convertColumnIndexToModel(col));
            Translator translator = translateService.getTranslator(field);
            if (translator instanceof NavigatingTranslator) {
                return (NavigatingTranslator) translator;
            }
        }
        return null;
    }

    public EntityTableModel getModel() {
        return model;
    }

    public JBTable getTable() {
        return table;
    }

    public void scrollTo(int row) {
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(new Rectangle(getTable().getCellRect(row, 0, true)));
    }

    public void scrollTo(Entity entity) {
        int row = model.indexOf(entity);
        if(row >= 0) {
            int viewRow = table.convertRowIndexToView(row);
            scrollTo(viewRow);
        }
    }

    public JComponent getQueryPanel() {
        return entityQueryPanel;
    }

    public Component getStatusComponent() {
        return statusPanel;
    }

    @Override
    public Object getData(@NonNls String s) {
        if(dataProvider != null) {
            Object value = dataProvider.getData(s);
            if(value != null) {
                return value;
            }
        }
        if("entity-list".equals(s)) {
            List<Entity> list = new LinkedList<Entity>();
            int[] selectedRows = table.getSelectedRows();
            for(int row: selectedRows) {
                list.add(model.getEntity(table.convertRowIndexToModel(row)));
            }
            return list;
        } else if("query".equals(s)) {
            return model.getFilter();
        } else if("master-entity".equals(s)) {
            return masterEntity;
        }
        return null;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
}
