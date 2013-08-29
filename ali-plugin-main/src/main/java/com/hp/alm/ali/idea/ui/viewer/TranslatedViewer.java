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

package com.hp.alm.ali.idea.ui.viewer;

import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.translate.NavigatingTranslator;
import com.hp.alm.ali.idea.translate.TranslateService;
import com.hp.alm.ali.idea.translate.Translator;
import com.hp.alm.ali.idea.translate.ValueCallback;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.hp.alm.ali.idea.model.Entity;
import com.intellij.openapi.project.Project;

import javax.swing.JTextPane;
import java.util.regex.Pattern;

public class TranslatedViewer extends TextViewer {

    private static Pattern NUMBER = Pattern.compile("^\\d+");

    private final TranslateService translateService;
    private final Entity entity;
    private final Field field;
    private final Pattern pattern;

    public TranslatedViewer(Project project, Entity entity, Field field, String value) {
        this(project, entity, field, value, NUMBER);
    }

    public TranslatedViewer(Project project, Entity entity, Field field, String value, Pattern pattern) {
        super(HTMLAreaField.createTextPane(value));
        this.field = field;
        this.pattern = pattern;
        this.translateService = project.getComponent(TranslateService.class);
        this.entity = entity;

        Translator translator = translateService.getTranslator(field);
        if(translator instanceof NavigatingTranslator) {
            ((JTextPane)getComponent()).addHyperlinkListener(((NavigatingTranslator)translator).getHyperlinkListener(entity));
        }

        setValue(value);
    }

    public void setValue(String value) {
        super.setValue(value);
        if(value != null && pattern.matcher(value).matches()) {
            translateService.translateAsync(field, value, true, new ValueCallback() {
                @Override
                public void value(String value) {
                    TranslatedViewer.super.setValue(value);
                }
            });
        }
    }
}
