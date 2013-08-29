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

import javax.swing.*;
import java.awt.*;

public class ScrollablePanel extends JPanel implements Scrollable {

    public static final int UNIT_INCREMENT = 26;
    public static final int BLOCK_INCREMENT = 53;

    public ScrollablePanel() {
    }

    public ScrollablePanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
        return UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
        return BLOCK_INCREMENT;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if(getParent() instanceof JViewport) {
            return (getParent().getHeight() > getPreferredSize().height);
        }
        return false;
    }
}
