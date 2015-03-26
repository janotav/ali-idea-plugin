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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.vcs.versionBrowser.DateFilterComponent;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.text.DateFormatUtil;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DateSelectorLabel extends JPanel implements LinkListener {

    private static final String ALL_DATES = "<all>";

    private final Project project;
    private final LinkLabel linkLabel;
    private Long lowerBound;
    private Long upperBound;

    private final List<ChangeListener> listeners;

    public DateSelectorLabel(Project project, String title) {
        this.project = project;

        listeners = new LinkedList<ChangeListener>();

        linkLabel = new LinkLabel(ALL_DATES, null);
        linkLabel.setListener(this, null);

        add(new JLabel(title + ":"));
        add(linkLabel);
    }

    @Override
    public void linkSelected(LinkLabel aSource, Object aLinkData) {
        Popup popup = new Popup();
        popup.show(aSource, 0, 20);
    }

    public Long getLowerBound() {
        return lowerBound;
    }

    public Long getUpperBound() {
        return upperBound;
    }

    public void addChangeListener(ChangeListener changeListener) {
        synchronized (listeners) {
            listeners.add(changeListener);
        }
    }

    private void fireChangeEvent(Object source) {
        synchronized (listeners) {
            for(ChangeListener listener: listeners) {
                listener.stateChanged(new ChangeEvent(source));
            }
        }
    }

    private static class Interval {

        private final String label;
        private final Long lowerBound;
        private final Long upperBound;

        private Interval(String label, Long lowerBound, Long upperBound) {
            this.label = label;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }

    private abstract class BaseActionListener implements ActionListener {

        protected abstract Interval calculateInterval(Calendar calendar);

        @Override
        public void actionPerformed(ActionEvent e) {
            Interval interval = calculateInterval(Calendar.getInstance());
            linkLabel.setText(interval.label);
            lowerBound = interval.lowerBound;
            upperBound = interval.upperBound;
            fireChangeEvent(DateSelectorLabel.this);
        }
    }

    private class Popup extends JPopupMenu {

        public Popup() {
            final JMenuItem all = new JMenuItem("All");
            all.addActionListener(new BaseActionListener() {
                @Override
                public Interval calculateInterval(Calendar calendar) {
                    return new Interval(ALL_DATES, null, null);
                }
            });
            add(all);
            add(new JSeparator());
            final JMenuItem today = new JMenuItem("Today");
            today.addActionListener(new BaseActionListener() {
                @Override
                public Interval calculateInterval(Calendar calendar) {
                    stripTime(calendar);
                    return new Interval(today.getText(), calendar.getTime().getTime(), null);
                }
            });
            add(today);
            final JMenuItem yesterday = new JMenuItem("Yesterday");
            yesterday.addActionListener(new BaseActionListener() {
                @Override
                public Interval calculateInterval(Calendar calendar) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    stripTime(calendar);
                    return new Interval(yesterday.getText(), calendar.getTime().getTime(), null);
                }
            });
            add(yesterday);
            // TODO: this sprint, previous sprint?
            final JMenuItem lastFourWeeks = new JMenuItem("Last 4 weeks");
            lastFourWeeks.addActionListener(new BaseActionListener() {
                @Override
                protected Interval calculateInterval(Calendar calendar) {
                    stripTime(calendar);
                    calendar.set(Calendar.DATE, -28);
                    return new Interval(lastFourWeeks.getText(), calendar.getTime().getTime(), null);
                }
            });
            add(lastFourWeeks);
            final JMenuItem thisYear = new JMenuItem("This year");
            thisYear.addActionListener(new BaseActionListener() {
                @Override
                protected Interval calculateInterval(Calendar calendar) {
                    stripTime(calendar);
                    calendar.set(Calendar.MONTH, 0);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    return new Interval(thisYear.getText(), calendar.getTime().getTime(), null);
                }
            });
            add(thisYear);
            add(new JSeparator());
            JMenuItem interval = new JMenuItem("Custom...");
            interval.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    final DateFormat dateFormat = DateFormatUtil.getDateFormat().getDelegate();
                    final DateFilterComponent dateFilterComponent = new DateFilterComponent(false, dateFormat);
                    if (upperBound != null) {
                        dateFilterComponent.setBefore(upperBound);
                    }
                    if (lowerBound != null) {
                        dateFilterComponent.setAfter(lowerBound);
                    }

                    final DialogBuilder builder = new DialogBuilder(project);
                    builder.setTitle("Specify Date Range");
                    builder.setOkActionEnabled(true);
                    builder.setOkOperation(new Runnable() {
                        @Override
                        public void run() {
                            if (dateFilterComponent.getAfter() >= 0) {
                                lowerBound = dateFilterComponent.getAfter();
                            } else {
                                lowerBound = null;
                            }
                            if (dateFilterComponent.getBefore() >= 0) {
                                upperBound = dateFilterComponent.getBefore();
                            } else {
                                upperBound = null;
                            }
                            builder.getDialogWrapper().close(0);

                            if (lowerBound == null && upperBound == null) {
                                linkLabel.setText(ALL_DATES);
                            } else if(lowerBound == null) {
                                linkLabel.setText("Before " + dateFormat.format(new Date(upperBound)));
                            } else if(upperBound == null) {
                                linkLabel.setText("After " + dateFormat.format(new Date(lowerBound)));
                            } else {
                                linkLabel.setText(dateFormat.format(new Date(lowerBound)) + "-" + dateFormat.format(new Date(upperBound)));
                            }

                            fireChangeEvent(DateSelectorLabel.this);
                        }
                    });
                    builder.setCenterPanel(dateFilterComponent.getPanel());
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            builder.showModal(true);
                        }
                    });
                }
            });
            add(interval);
        }

        private void stripTime(Calendar calendar) {
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }
    }
}
