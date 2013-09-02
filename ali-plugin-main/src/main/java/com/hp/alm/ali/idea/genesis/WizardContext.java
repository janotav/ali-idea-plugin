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

package com.hp.alm.ali.idea.genesis;

import com.hp.alm.ali.idea.genesis.checkout.Checkout;
import com.hp.alm.ali.rest.client.RestClient;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

public class WizardContext {

    public JLabel locationLbl;
    public JTextField location;
    public JLabel domainLbl;
    public JComboBox domain;
    public JLabel projectLbl;
    public JComboBox project;
    public JLabel usernameLbl;
    public JTextField username;
    public JLabel passwordLbl;
    public JPasswordField password;
    public JLabel releaseLbl;
    public JComboBox release;
    public JLabel repositoryLbl;
    public JComboBox repository;
    public JLabel branchLbl;
    public JComboBox branch;
    public JLabel targetLbl;
    public TextFieldWithBrowseButton targetBtn;
    public JLabel targetFullLbl;
    public JComboBox targetFull;
    public Checkout checkout;
    public RestClient client;
    public JComponent panel;

}
