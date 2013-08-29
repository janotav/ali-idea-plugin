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

package com.hp.alm.ali.idea.navigation;

import com.intellij.openapi.project.Project;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class NavigationListener implements HyperlinkListener {

    private Project project;

    public NavigationListener(Project project) {
        this.project = project;
    }

    public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
        if(hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            for(Recognizer recognizer: NavigationDecorator.recognizers) {
                if(recognizer.navigate(project, hyperlinkEvent.getDescription())) {
                    break;
                }
            }
        }
    }
}
