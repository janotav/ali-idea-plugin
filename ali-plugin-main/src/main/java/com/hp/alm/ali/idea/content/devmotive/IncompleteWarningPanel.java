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

package com.hp.alm.ali.idea.content.devmotive;

import com.hp.alm.ali.idea.ui.WarningPanel;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;

import javax.swing.JTextPane;
import javax.swing.event.HyperlinkListener;
import java.awt.Color;

public class IncompleteWarningPanel extends WarningPanel {

    public IncompleteWarningPanel(Color background) {
        super(HTMLAreaField.createTextPane(""), background, false, false);
    }

    public void setState(int loaded, int total) {
        if (loaded < total) {
            ((JTextPane)getComponent()).setText(getIncompleteText(loaded, total));
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    public void addHyperLinkListener(HyperlinkListener listener) {
        ((JTextPane) getComponent()).addHyperlinkListener(listener);
    }

    public void markFailed() {
        ((JTextPane)getComponent()).setText("<html><body>Unable to retrieve revision history, information is not available.</body></html>.");
        setVisible(true);
    }

    private static String getIncompleteText(int loaded, int total) {
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body>Information incomplete (");
        buf.append(loaded);
        buf.append(" out of ");
        buf.append(total);
        buf.append(" revisions processed). Either limit the revision scope or load <a href=\"more\">more</a>.</body></html>.");
        buf.append("</body></html>.");
        return buf.toString();
    }
}
