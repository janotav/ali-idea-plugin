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

package com.hp.alm.ali.idea.genesis.steps;

import com.hp.alm.ali.idea.genesis.WizardContext;
import com.intellij.ide.wizard.StepAdapter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class GenesisStep extends StepAdapter {

    private List<? extends JComponent> myControls;
    private GenesisStep previous;
    private GenesisStep next;
    protected WizardContext ctx;

    public GenesisStep(GenesisStep previous, WizardContext ctx, List<? extends JComponent> myControls) {
        this.ctx = ctx;
        this.myControls = myControls;
        this.previous = previous;
        if(previous != null) {
            previous.next = this;
        }
    }

    public void _init() {
        for(JComponent control: myControls) {
            control.setVisible(true);
            control.setEnabled(true);
        }
        if(previous != null) {
            for(JComponent control: previous.myControls) {
                control.setEnabled(false);
            }
        }
        if(next != null) {
            for(JComponent control: next.myControls) {
                control.setVisible(false);
            }
        }
        if(!myControls.isEmpty()) {
            myControls.get(0).requestFocusInWindow();
        }
    }

    public boolean isImplicitChoice() {
        return false;
    }

    public boolean isNextAvailable() {
        return true;
    }

    public JComponent getComponent() {
        return ctx.panel;
    }

    public Icon getIcon() {
        return null;
    }

}
