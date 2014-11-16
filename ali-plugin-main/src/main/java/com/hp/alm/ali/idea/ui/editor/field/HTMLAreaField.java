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

import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.impl.SpellCheckerManager;
import com.hp.alm.ali.idea.ui.html.HTMLLetterWrappingEditorKit;
import com.hp.alm.ali.idea.ui.html.BodyLimitCaretListener;
import com.hp.alm.ali.idea.ui.html.InsertHardBreakAction;
import com.hp.alm.ali.idea.ui.NonAdjustingCaret;
import com.hp.alm.ali.idea.navigation.NavigationDecorator;
import com.hp.alm.ali.idea.navigation.NavigationListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class HTMLAreaField extends TextField {
    public static Pattern HTML_CONTENT_RX = Pattern.compile("^\\s*<html>");

    private static final Set<String> allowedElements = new HashSet<String>(Arrays.asList("html", "head", "body", "p", "p-implied", "font", "span", "content", "br"));

    private JBScrollPane pane;

    public HTMLAreaField(Project project, String label, String value, boolean required, boolean editable) {
        super(createTextPane(project, value, editable, false), label, required, editable);
    }

    public static JTextPane createTextPane(String value) {
        return createTextPane(value, false);
    }

    public static JTextPane createTextPane(String value, boolean editable) {
        return createTextPane(null, value, editable, false);
    }

    public static JTextPane createTextPane(final Project project, String value, boolean editable, boolean navigation) {
        JTextPane desc = new JTextPane();
        desc.addHyperlinkListener(new NavigationListener(project));
        enableCapability(desc, project, value, editable, navigation);
        return desc;
    }

    public static void enableCapability(final JTextPane desc, Project project, String value, boolean editable, boolean navigation) {
        value = removeSmallFont(value);
        HTMLEditorKit kit = new HTMLLetterWrappingEditorKit();
        desc.setEditorKit(kit);
        desc.setDocument(kit.createDefaultDocument());
        if(!editable && navigation) {
            value = NavigationDecorator.explodeHtml(project, value);
        }
        desc.setText(value);
        if(!editable) {
            desc.setCaret(new NonAdjustingCaret());
        }
        desc.addCaretListener(new BodyLimitCaretListener(desc));
        if(editable) {
            String element = checkElements(desc.getDocument().getDefaultRootElement());
            if(element != null) {
                desc.setToolTipText("Found unsupported element '"+element+"', editing is disabled.");
                editable = false;
            }
        }
        desc.setEditable(editable);

        if (editable && SpellCheckerManager.isAvailable() && ApplicationManager.getApplication().getComponent(AliConfiguration.class).spellChecker) {
            desc.getDocument().addDocumentListener(new SpellCheckDocumentListener(project, desc));
        }

        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)desc.getDocument()).getStyleSheet().addRule(bodyRule);

        // AGM uses plain "p" to create lines, we need to avoid excessive spacing this by default creates
        String paragraphRule = "p { margin-top: 0px; }";
        ((HTMLDocument)desc.getDocument()).getStyleSheet().addRule(paragraphRule);

        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // following is needed to make copy work in the IDE
                try {
                    StringSelection selection = new StringSelection(desc.getText(desc.getSelectionStart(), desc.getSelectionEnd() - desc.getSelectionStart()));
                    CopyPasteManager.getInstance().setContents(selection);
                } catch (Exception ex) {
                    // no clipboard, so what
                }
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts(IdeActions.ACTION_COPY)), desc);
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // avoid pasting non-supported HTML markup by always converting to plain text
                Transferable contents = CopyPasteManager.getInstance().getContents();
                try {
                    desc.getActionMap().get(DefaultEditorKit.cutAction).actionPerformed(null);
                    desc.getDocument().insertString(desc.getSelectionStart(), (String) contents.getTransferData(DataFlavor.stringFlavor), null);
                } catch (Exception ex) {
                    // no clipboard, so what
                }
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts(IdeActions.ACTION_PASTE)), desc);
        installNavigationShortCuts(desc);

    }

    public static void installNavigationShortCuts(final JTextPane desc) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // default action moves to the end of the document - override
                desc.getActionMap().get(DefaultEditorKit.endLineAction).actionPerformed(null);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts(IdeActions.ACTION_EDITOR_MOVE_LINE_END)), desc);
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // default action moves to the beginning of the document - override
                desc.getActionMap().get(DefaultEditorKit.beginLineAction).actionPerformed(null);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keymap.getShortcuts(IdeActions.ACTION_EDITOR_MOVE_LINE_START)), desc);
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // default action moves to the end of the document - override
                desc.getActionMap().get(DefaultEditorKit.selectionEndLineAction).actionPerformed(null);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_MASK)), desc);
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // default action moves to the beginning of the document - override
                desc.getActionMap().get(DefaultEditorKit.selectionBeginLineAction).actionPerformed(null);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_MASK)), desc);
        new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                // when editing html insert hard break
                new InsertHardBreakAction().actionPerformed(null);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), desc);
    }

    private static String checkElements(Element element) {
        if(!allowedElements.contains(element.getName())) {
            return element.getName();
        }
        for(int i = 0; i < element.getElementCount(); i++) {
            String failed;
            if((failed = checkElements(element.getElement(i))) != null) {
                return failed;
            }
        }
        return null;
    }

    public static String removeSmallFont(String htmlContent) {
        if(htmlContent != null && HTML_CONTENT_RX.matcher(htmlContent).find()) {
            htmlContent = htmlContent.replaceAll("(<span )(style=\"font-size:8pt\">)", "$1qc$2");
            htmlContent = htmlContent.replaceAll("\r\n", "\n");
            // ALM uses <div> while swing <p> to make lines
            htmlContent = Pattern.compile("<div([ >].*?</)div>", Pattern.DOTALL).matcher(htmlContent).replaceAll("<p style=\"margin-top: 0\"$1p>");
        }
        return htmlContent;
    }

    @Override
    public Component getComponent() {
        if(pane == null) {
            pane = new JBScrollPane(super.getComponent(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pane.setPreferredSize(new Dimension(600, 30));
            pane.setMinimumSize(new Dimension(300, 30));
        }
        return pane;
    }

    @Override
    public boolean isDisableDefaultAction() {
        return true;
    }

    @Override
    public String getValue() {
        return toQcHtml(super.getValue());
    }

    @Override
    public boolean hasChanged() {
        return !super.getValue().equals(getOriginalValue());
    }

    @Override
    public void setOriginalValue(String value) {
        super.setOriginalValue(createTextPane(value).getText());
    }

    public static String toPlainText(String html, boolean compact) {
        String str = html.replaceAll("[\\r\\n]", "")
                .replaceAll("<[Bb][Rr]\\s*/?>", "\n")
                // following is needed for analyze stack trace to work
                // because QC editor uses <div> for line breaking
                .replaceFirst("<body>\\s*<div align=\"left\">", "") // avoid extra new line at the beginning
                .replaceAll("(<div align=\"left\">)", "\n$1")
                .replaceAll("</?[^>]+/?>", "")
                .replaceAll(" {2,}", " ")
                .replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&#160;", " ");
        if(compact) {
            str = str.replaceAll("[\\r\\n]", " ").replaceAll(" {2,}", " ");
        }
        return str;
    }

    public static String toQcHtml(String html) {
        // try to revert back constructs not compatible with QC HTML
        html = Pattern.compile("^\\s*<html>\\s*<head>.*?</head>", Pattern.DOTALL).matcher(html).replaceAll("<html>");
        html = html.replaceAll("(<span )qc(style=\"font-size:8pt\">)", "$1$2");
        html = Pattern.compile("<p style=\"margin-top: 0\"(.*?</)p>", Pattern.DOTALL).matcher(html).replaceAll("<div$1div>");
        return html;
    }

    public static String toHtml(String plain, boolean body) {
        StringBuffer buf = new StringBuffer();
        if(body) {
            buf.append("<html><body>");
        }
        buf.append(StringEscapeUtils.escapeHtml(plain).replace("\n","<br>"));
        if(body) {
            buf.append("</body></html>");
        }
        return buf.toString();
    }

    public static String toHtml(String plain) {
        return toHtml(plain, true);
    }
}
