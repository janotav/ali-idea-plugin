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

package com.hp.alm.ali.idea.ui.editor.field;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UploadField extends BaseField {

    // static field: share the directory across all uploads
    private static VirtualFile lastDir;

    private TextFieldWithBrowseButton textFieldWithBrowseButton;

    public UploadField(String label, boolean required, boolean editable) {
        super(label, required, "");

        textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        textFieldWithBrowseButton.getTextField().setEditable(false);
        textFieldWithBrowseButton.getTextField().getDocument().addDocumentListener(new MyDocumentListener(this));

        if(editable) {
            textFieldWithBrowseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    VirtualFile[] file = FileChooser.chooseFiles(new FileChooserDescriptor(true, false, true, true, false, false), textFieldWithBrowseButton.getTextField(), null, lastDir);
                    if(file.length > 0) {
                        textFieldWithBrowseButton.getTextField().setText(file[0].getPath());
                        lastDir = file[0].getParent();
                    }
                }
            });
        } else {
            textFieldWithBrowseButton.setEnabled(false);
        }
    }

    public Component getComponent() {
        return textFieldWithBrowseButton;
    }

    public String getValue() {
        return textFieldWithBrowseButton.getText();
    }

    @Override
    public void setValue(String value) {
        textFieldWithBrowseButton.setText(value);
    }
}
