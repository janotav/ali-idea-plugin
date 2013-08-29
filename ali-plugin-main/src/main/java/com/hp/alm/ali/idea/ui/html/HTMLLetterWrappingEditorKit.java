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

package com.hp.alm.ali.idea.ui.html;

import javax.swing.SizeRequirements;
import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;

public class HTMLLetterWrappingEditorKit extends HTMLEditorKit {
    public ViewFactory getViewFactory(){
        return new HTMLLetterWrappingFactory();
     }

    private static class LetterWrappingParagraphView extends javax.swing.text.html.ParagraphView {

        public LetterWrappingParagraphView(Element element) {
            super(element);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
            if (r == null) {
                r = new SizeRequirements();
            }
            float pref = layoutPool.getPreferredSpan(axis);
            float min = layoutPool.getMinimumSpan(axis);
            r.minimum = (int)min;
            r.preferred = Math.max(r.minimum, (int) pref);
            r.maximum = Integer.MAX_VALUE;
            r.alignment = 0.5f;
            return r;
        }
    }

    private static class HTMLLetterWrappingFactory extends HTMLEditorKit.HTMLFactory {

        public View create(Element e){
            View view = super.create(e);
            if (view instanceof ParagraphView) {
                return new LetterWrappingParagraphView(e);
            } else {
                return view;
            }
         }
    }
}
