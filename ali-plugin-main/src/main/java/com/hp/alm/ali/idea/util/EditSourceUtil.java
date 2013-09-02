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

package com.hp.alm.ali.idea.util;

import com.intellij.navigation.NavigationItem;
import org.jetbrains.annotations.TestOnly;

public class EditSourceUtil {

    private Navigator navigator = new DefaultNavigator();

    public void navigate(NavigationItem item, boolean requestFocus, boolean useCurrentWindow) {
        navigator.navigate(item, requestFocus, useCurrentWindow);
    }

    @TestOnly
    public void _restore() {
        navigator = new DefaultNavigator();
    }

    @TestOnly
    public void _setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public static interface Navigator {

        public void navigate(com.intellij.navigation.NavigationItem item, boolean requestFocus, boolean useCurrentWindow);

    }

    private static class DefaultNavigator implements Navigator {

        @Override
        public void navigate(NavigationItem item, boolean requestFocus, boolean useCurrentWindow) {
            com.intellij.ide.util.EditSourceUtil.navigate(item, requestFocus, useCurrentWindow);
        }
    }
}
