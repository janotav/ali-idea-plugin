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

package com.hp.alm.ali.idea.cfg;

import com.hp.alm.ali.idea.services.WeakListeners;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class EntityFields implements JDOMSerialization {

    private WeakListeners<ColumnsChangeListener> listeners = new WeakListeners<ColumnsChangeListener>();
    private final List<String> columns = new ArrayList<String>();

    @Override
    public Element toElement(String name) {
        Element target = new Element(name);
        Element colsElem = new Element("columns");
        for(String column: columns) {
            Element colElem = new Element("column");
            colElem.setAttribute("name", column);
            colsElem.addContent(colElem);
        }
        target.addContent(colsElem);
        return target;
    }

    @Override
    public void fromElement(Element element) {
        Element colsElem = element.getChild("columns");
        if(colsElem != null) {
            columns.clear();
            for(Element child: (List<Element>)colsElem.getChildren("column")) {
                columns.add(child.getAttributeValue("name"));
            }
        }
    }

    public synchronized List<String> getColumns() {
        return new ArrayList<String>(columns);
    }

    public void setColumns(List<String> columns) {
        synchronized(this) {
            this.columns.clear();
            this.columns.addAll(columns);
        }
        fireColumnsChanged(null);
    }

    public void addColumn(String column) {
        synchronized(this) {
            columns.add(column);
        }
        fireColumnsChanged(column);
    }

    public void addColumns(List<String> columns) {
        boolean changed = false;
        synchronized(this) {
            for (String column: columns) {
                if (!this.columns.contains(column)) {
                    this.columns.add(column);
                    changed = true;
                }
            }
        }
        if (changed) {
            fireColumnsChanged(null);
        }
    }

    public void removeColumn(String column) {
        synchronized(this) {
            if(!columns.remove(column)) {
                return;
            }
        }
        fireColumnsChanged(null);
    }

    public void moveColumn(String column, int offset) {
        synchronized(this) {
            int i = columns.indexOf(column);
            if(i >= 0 && i + offset < columns.size() && i + offset >= 0) {
                columns.remove(i);
                columns.add(i + offset, column);
            } else {
                return;
            }
        }
        fireColumnsChanged(column);
    }

    public void addColumnsChangeListener(ColumnsChangeListener listener) {
        listeners.add(listener);
    }

    public void removeColumnsChangeListener(ColumnsChangeListener listener) {
        listeners.remove(listener);
    }

    void fireColumnsChanged(final String columnToFocus) {
        listeners.fire(new WeakListeners.Action<ColumnsChangeListener>() {
            @Override
            public void fire(ColumnsChangeListener listener) {
                listener.columnsChanged(columnToFocus);
            }
        });
    }

    public static interface ColumnsChangeListener {

        void columnsChanged(String columnToFocus);

    }
}
