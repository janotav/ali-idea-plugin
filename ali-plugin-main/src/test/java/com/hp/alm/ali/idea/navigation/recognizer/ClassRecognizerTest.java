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

package com.hp.alm.ali.idea.navigation.recognizer;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.navigation.Candidate;
import com.hp.alm.ali.idea.util.EditSourceUtil;
import com.hp.alm.ali.idea.util.FileEditorManager;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.textarea.TextComponentEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.IdeaTestUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.JTextArea;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ClassRecognizerTest extends IntellijTest {

    public ClassRecognizerTest() {
        super(ServerVersion.AGM);
    }

    @After
    public void postClean() {
        getComponent(EditSourceUtil.class)._restore();
        getComponent(FileEditorManager.class)._restore();
    }

    @Test
    public void testNavigate_class() {
        handler.async(2);
        getComponent(EditSourceUtil.class)._setNavigator(new EditSourceUtil.Navigator() {
            @Override
            public void navigate(final NavigationItem item, final boolean requestFocus, final boolean useCurrentWindow) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("PsiClass:Clazz", item.toString());
                        Assert.assertTrue(requestFocus);
                        Assert.assertTrue(useCurrentWindow);
                    }
                });
            }
        });
        final TextComponentEditor editor = new TextComponentEditor(getProject(), new JTextArea("1\n2\n3\n4\n5\n"));
        editor.getSelectionModel().setSelection(1, 2);
        final int line = 3;
        getComponent(FileEditorManager.class)._setSelector(new FileEditorManager.Selector() {
            @Override
            public Editor getSelectedTextEditor() {
                handler.done();
                return editor;
            }
        });

        createTestClazz();
        boolean result = new ReadAction<Boolean>() {
            @Override
            protected void run(Result<Boolean> result) throws Throwable {
                String hyperlink = new ClassCandidate(1, 100, 10, 90, "test.Clazz", "Clazz", line, null).createLink(getProject());
                result.setResult(new MyRecognizer().navigate(getProject(), hyperlink));
            }
        }.execute().getResultObject();
        Assert.assertTrue(result);
        Assert.assertEquals(new LogicalPosition(line - 1, 0), editor.getCaretModel().getLogicalPosition());
        Assert.assertNull(editor.getSelectionModel().getSelectedText());
    }

    @Test
    public void testNavigate_method() {
        handler.async();
        getComponent(EditSourceUtil.class)._setNavigator(new EditSourceUtil.Navigator() {
            @Override
            public void navigate(final NavigationItem item, final boolean requestFocus, final boolean useCurrentWindow) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("PsiMethod:one", item.toString());
                        Assert.assertTrue(requestFocus);
                        Assert.assertTrue(useCurrentWindow);
                    }
                });
            }
        });

        createTestClazz();
        boolean result = new ReadAction<Boolean>() {
            @Override
            protected void run(Result<Boolean> result) throws Throwable {
                String hyperlink = new ClassCandidate(1, 100, 10, 90, "test.Clazz", "Clazz", 0, "one").createLink(getProject());
                result.setResult(new MyRecognizer().navigate(getProject(), hyperlink));
            }
        }.execute().getResultObject();
        Assert.assertTrue(result);
    }

    @Test
    public void testNavigate_classNonQualified() {
        handler.async(2);
        getComponent(EditSourceUtil.class)._setNavigator(new EditSourceUtil.Navigator() {
            @Override
            public void navigate(final NavigationItem item, final boolean requestFocus, final boolean useCurrentWindow) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("PsiClass:Clazz", item.toString());
                        Assert.assertTrue(requestFocus);
                        Assert.assertTrue(useCurrentWindow);
                    }
                });
            }
        });
        final TextComponentEditor editor = new TextComponentEditor(getProject(), new JTextArea("1\n2\n3\n4\n5\n"));
        editor.getSelectionModel().setSelection(1, 2);
        final int line = 3;
        getComponent(FileEditorManager.class)._setSelector(new FileEditorManager.Selector() {
            @Override
            public Editor getSelectedTextEditor() {
                handler.done();
                return editor;
            }
        });

        createTestClazz();
        boolean result = new ReadAction<Boolean>() {
            @Override
            protected void run(Result<Boolean> result) throws Throwable {
                String hyperlink = new ClassCandidate(1, 100, 10, 90, "hello.Clazz", "Clazz", line, null).createLink(getProject());
                result.setResult(new MyRecognizer().navigate(getProject(), hyperlink));
            }
        }.execute().getResultObject();
        Assert.assertTrue(result);
        Assert.assertEquals(new LogicalPosition(line - 1, 0), editor.getCaretModel().getLogicalPosition());
        Assert.assertNull(editor.getSelectionModel().getSelectedText());
    }

    @Test
    public void testNavigate_negative() {
        getComponent(EditSourceUtil.class)._setNavigator(new EditSourceUtil.Navigator() {
            @Override
            public void navigate(NavigationItem item, boolean requestFocus, boolean useCurrentWindow) {
                handler.fail("Not expected");
            }
        });

        createTestClazz();
        boolean result = new ReadAction<Boolean>() {
            @Override
            protected void run(Result<Boolean> result) throws Throwable {
                result.setResult(new MyRecognizer().navigate(getProject(), "goto:c=test.Clazz2&f=Clazz2&l=3"));
            }
        }.execute().getResultObject();
        Assert.assertTrue(result);
    }

    private void createTestClazz() {
        new WriteCommandAction(getProject()) {
            @Override
            protected void run(Result result) throws Throwable {
                Module module = ModuleManager.getInstance(getProject()).findModuleByName("navigation_decorator_java");
                if(module == null) {
                    File tempFile = File.createTempFile("test", "");
                    tempFile.delete();
                    tempFile.mkdir();
                    final VirtualFile dummyRoot = VirtualFileManager.getInstance().findFileByUrl("file://" + tempFile.getAbsolutePath());
                    dummyRoot.refresh(false, false);
                    List<String> list = Arrays.asList("src/test/Clazz.java".split("/"));
                    String dirPath = StringUtil.join(list.subList(0, list.size() - 1), "/");
                    VirtualFile dir = VfsUtil.createDirectories(dummyRoot.getPath() + dirPath);

                    VirtualFile vFile = dir.findOrCreateChildData(this, "Clazz.java");
                    VfsUtil.saveText(vFile, "package test; public class Clazz { public void one() {} }");

                    module = ApplicationManager.getApplication().runWriteAction(new Computable<Module>() {
                        @Override
                        public Module compute() {
                            return ModuleManager.getInstance(getProject()).newModule("class_recognizer.iml", StdModuleTypes.JAVA.getId());
                        }
                    });
                    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                    ModifiableRootModel rootModel = rootManager.getModifiableModel();
                    rootModel.setSdk(IdeaTestUtil.getMockJdk17());

                    ContentEntry contentEntry = rootModel.addContentEntry(dir.getParent());
                    contentEntry.addSourceFolder(dir.getParent(), false);
                    rootModel.commit();
                }
            }
        }.execute();
    }

    private static class MyRecognizer extends ClassRecognizer {

        @Override
        public void recognize(String content, List<Candidate> candidates) {
        }
    }
}
