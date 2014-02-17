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

package com.hp.alm.ali.idea.ui.entity.query;

import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.FilterListener;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.Metadata;
import com.hp.alm.ali.idea.ui.MultipleItemsDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

class OrderPanel extends JPanel {
    private Metadata metadata;

    private static Icon moveDownIcon = IconLoader.getIcon("/actions/moveDown.png");
    private static Icon moveUpIcon = IconLoader.getIcon("/actions/moveUp.png");
    private static Icon sortDescIcon = IconLoader.getIcon("/actions/sortDesc.png");
    private static Icon sortAscIcon = IconLoader.getIcon("/actions/sortAsc.png");
    private static Icon addIcon = IconLoader.getIcon("/general/add.png");
    private static Icon removeIcon = IconLoader.getIcon("/general/remove.png");

    private JPanel propertyNamePanel;
    private JPanel propertyRemovePanel;
    private JPanel directionPanel;
    private JPanel moveDownPanel;
    private JPanel moveUpPanel;

    final private java.util.List<FilterListener> queryListeners = new LinkedList<FilterListener>();

    public OrderPanel(final Project project, final EntityQuery query, final Metadata metadata) {
        super(new GridBagLayout());
        this.metadata = metadata;

        setBorder(new EmptyBorder(10, 10, 10, 10));

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        c.gridwidth = 2;
        add(boldLabel("Property"), c);
        c.gridx = 2;
        c.weightx = 0.1;
        c.gridwidth = 1;
        add(boldLabel("Direction"), c);
        c.gridx++;
        c.gridwidth = 2;
        c.weightx = 0.05;
        add(boldLabel("Position"), c);
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;

        c.fill = GridBagConstraints.BOTH;
        propertyNamePanel = createVBox();
        add(propertyNamePanel, c);
        c.gridx++;
        propertyRemovePanel = createVBox();
        add(propertyRemovePanel, c);
        c.gridx++;
        directionPanel = createVBox();
        add(directionPanel, c);
        c.gridx++;
        moveDownPanel = createVBox();
        add(moveDownPanel, c);
        c.gridx++;
        moveUpPanel = createVBox();
        add(moveUpPanel, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        final LinkLabel addProperty = new LinkLabel("Add Columns", addIcon);
        addProperty.setListener(new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object o) {
                LinkedHashMap<String,SortOrder> order = query.getOrder();
                List<Field> available = new ArrayList<Field>(metadata.getAllFields().values());
                for(Iterator<Field> it = available.iterator(); it.hasNext(); ) {
                    Field field = it.next();
                    if(!field.isCanFilter() || field.isNoSort() || order.containsKey(field.getName())) {
                        it.remove();
                    }
                }
                final ArrayList<Field> sortedFields = new ArrayList<Field>(available);
                Collections.sort(sortedFields, Field.LABEL_COMPARATOR);
                ArrayList<String> toAdd = new ArrayList<String>();
                MultipleItemsDialog dialog = new MultipleItemsDialog(project, "Column", true, sortedFields, toAdd);
                dialog.setVisible(true);
                if(dialog.isOk()) {
                    for(String column: toAdd) {
                        addSortColumn(-1, column, SortOrder.ASCENDING, query);
                        query.addOrder(column, SortOrder.ASCENDING);
                        fireQueryChangedEvent(query);
                    }
                }
            }
        }, null);
        add(addProperty, c);

        c.gridx = 6;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        add(new JPanel(), c);

        LinkedHashMap<String, SortOrder> order = query.getOrder();
        ArrayList<String> props = new ArrayList<String>(order.keySet());
        for (String prop : props) {
            addSortColumn(-1, prop, order.get(prop), query);
        }
    }

    private JPanel createVBox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JPanel createCell(JComponent comp) {
        return createCell(comp, new BorderLayout(), BorderLayout.CENTER);
    }

    private JPanel createCell(JComponent comp, LayoutManager layout, Object positioning) {
        JPanel cell = new JPanel(layout) {
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 20);
            }
        };
        cell.add(comp, positioning);
        return cell;
    }

    private void removeSortColumn(int idx) {
        propertyNamePanel.remove(idx);
        propertyRemovePanel.remove(idx);
        directionPanel.remove(idx);
        moveUpPanel.remove(idx);
        moveDownPanel.remove(idx);
        int count = moveUpPanel.getComponentCount();
        if(count > 0) {
            if(idx == 0) {
                ((JLabel)getCellContent(moveUpPanel, 0)).setIcon(null);
            }
            if(idx == count) {
                ((JLabel)getCellContent(moveDownPanel, -1)).setIcon(null);
            }
        }
    }

    private void addSortColumn(int idx, final String prop, SortOrder dir, final EntityQuery query) {
        if(idx < 0) {
            idx = propertyNamePanel.getComponentCount() + idx + 1;
        }
        propertyNamePanel.add(createCell(new JLabel(metadata.getAllFields().get(prop).getLabel())), idx);
        propertyRemovePanel.add(createCell(new LinkLabel("", removeIcon, new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object o) {
                int idx = Arrays.asList(propertyRemovePanel.getComponents()).indexOf(linkLabel.getParent());
                removeSortColumn(idx);
                query.removeOrder(prop);
                fireQueryChangedEvent(query);
            }
        }, null)), idx);
        directionPanel.add(createCell(createDirectionLabel(dir, new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object jLabel) {
                SortOrder newOrder = query.getOrder().get(prop) == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
                ((JLabel)jLabel).setIcon(newOrder == SortOrder.ASCENDING? sortAscIcon: sortDescIcon);
                query.addOrder(prop, newOrder);
                fireQueryChangedEvent(query);
            }
        }), new FlowLayout(FlowLayout.CENTER), null), idx);
        LinkListener moveUpListener = new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object jLabel) {
                JLabel link = (JLabel) jLabel;
                int idx = Arrays.asList(moveUpPanel.getComponents()).indexOf(link.getParent());
                moveOrder(idx, query, prop, -1);
            }
        };
        if(idx > 0) {
            ((JLabel)getCellContent(moveDownPanel, idx - 1)).setIcon(moveDownIcon);
            moveUpPanel.add(createCell(createLinkLabel(moveUpIcon, moveUpListener, SwingConstants.LEFT)), idx);
        } else {
            moveUpPanel.add(createCell(createLinkLabel(null, moveUpListener, SwingConstants.LEFT)), idx);
        }
        LinkListener moveDownListener = new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object jLabel) {
                JLabel link = (JLabel) jLabel;
                int idx = Arrays.asList(moveDownPanel.getComponents()).indexOf(link.getParent());
                moveOrder(idx, query, prop, 1);
            }
        };
        if(idx < moveDownPanel.getComponentCount()) {
            ((JLabel)getCellContent(moveUpPanel, idx + 1)).setIcon(moveUpIcon);
            moveDownPanel.add(createCell(createLinkLabel(moveDownIcon, moveDownListener, SwingConstants.RIGHT)), idx);
        } else {
            moveDownPanel.add(createCell(createLinkLabel(null, moveDownListener, SwingConstants.RIGHT)), idx);
        }
    }

    private void moveOrder(int idx, EntityQuery query, String prop, int ofs) {
        removeSortColumn(idx);
        addSortColumn(idx + ofs, prop, query.getOrder().get(prop), query);
        OrderPanel.this.revalidate();
        OrderPanel.this.repaint();

        // change order in LinkedHashMap, hmm...
        LinkedHashMap<String, SortOrder> order = query.getOrder();
        LinkedList<String> keys = new LinkedList<String>(order.keySet());
        keys.add(idx + ofs, keys.remove(idx));
        LinkedHashMap<String, SortOrder> newOrder = new LinkedHashMap<String, SortOrder>();
        for(String key: keys) {
            newOrder.put(key, order.get(key));
        }
        query.setOrder(newOrder);
        fireQueryChangedEvent(query);
    }

    private JLabel createLinkLabel(Icon icon, final LinkListener linkListener, int alignment) {
        // LinkLabel didn't work:
        // - neither setHorizontalAlignment nor setIcon works correctly (part of the logic uses cached values)
        // - sometimes hand didn't appear although mouse was over the image (when image too small?)
        final JLabel linkLabel = new JLabel(icon, alignment);
        linkLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if(linkLabel.getIcon() != null) {
                    linkListener.linkSelected(null, linkLabel);
                }
            }

            public void mouseEntered(MouseEvent mouseEvent) {
                if(linkLabel.getIcon() != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            public void mouseExited(MouseEvent mouseEvent) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        return linkLabel;
    }

    private JLabel createDirectionLabel(SortOrder order, final LinkListener linkListener) {
        return createLinkLabel(order == SortOrder.ASCENDING ? sortAscIcon : sortDescIcon, linkListener, SwingConstants.CENTER);
    }

    private Component getCellContent(JPanel panel, int idx) {
        if(idx >= 0) {
            return ((Container)panel.getComponent(idx)).getComponent(0);
        } else {
            return ((Container)panel.getComponent(panel.getComponentCount()+idx)).getComponent(0);
        }
    }

    private JLabel boldLabel(String label) {
        final JLabel ret = new JLabel(label, SwingConstants.CENTER);
        ret.setFont(ret.getFont().deriveFont(Font.BOLD));
        ret.setBorder(BorderFactory.createEtchedBorder());
        return ret;
    }

    public void addQueryListener(FilterListener listener) {
        synchronized (queryListeners) {
            queryListeners.add(listener);
        }
    }

    private void fireQueryChangedEvent(EntityQuery query) {
        synchronized (queryListeners) {
            for(FilterListener listener: queryListeners) {
                listener.filterChanged(query);
            }
        }
    }
}
