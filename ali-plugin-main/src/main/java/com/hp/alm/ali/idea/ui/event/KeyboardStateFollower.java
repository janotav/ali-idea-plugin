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

package com.hp.alm.ali.idea.ui.event;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyboardStateFollower implements KeyboardState, KeyEventDispatcher {

    private static final int CTRL_KEY = 0;
    private static final int SHIFT_KEY = 1;

    private List<Boolean> mask = new ArrayList<Boolean>(Arrays.asList(false, false));
    private static KeyboardStateFollower instance;
    static {
        instance = new KeyboardStateFollower();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(instance);
    }

    public static KeyboardState getState() {
        return instance;
    }

    private KeyboardStateFollower() {
    }

    @Override
    public boolean isCtrlDown() {
        return mask.get(CTRL_KEY);
    }

    @Override
    public boolean isShiftDown() {
        return mask.get(SHIFT_KEY);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        mask.set(CTRL_KEY, keyEvent.isControlDown());
        mask.set(SHIFT_KEY, keyEvent.isShiftDown());
        return false;
    }
}
