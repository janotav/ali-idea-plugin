// (C) Copyright 2003-2013 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;

public class MessagesUtil {

    public static void showErrorDialogLater(final Project project, final String message, final String title) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                Messages.showErrorDialog(project, message, title);
            }
        });
    }
}
