// (C) Copyright 2003-2014 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.ui;

import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.idea.services.AbstractCachingService;
import com.hp.alm.ali.idea.services.WorkspaceService;
import com.hp.alm.ali.idea.ui.editor.field.HTMLAreaField;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Color;
import java.util.Map;

public class WorkspaceWarningPanel extends WarningPanel implements HyperlinkListener, ServerTypeListener {

    private RestService restService;
    private WorkspaceService workspaceService;
    private int workspaceId;
    private String workspaceName;

    public WorkspaceWarningPanel(Project project, final int workspaceId, Color background, boolean canClose) {
        super(HTMLAreaField.createTextPane(""), background, canClose, false);
        this.workspaceId = workspaceId;
        this.restService = project.getComponent(RestService.class);
        this.workspaceService = project.getComponent(WorkspaceService.class);

        ((JTextPane)getComponent()).addHyperlinkListener(this);
        project.getComponent(RestService.class).addServerTypeListener(this);

        connectedTo(restService.getServerTypeIfAvailable());
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            workspaceService.selectWorkspace(workspaceId, workspaceName);
        }
    }

    @Override
    public void connectedTo(final ServerType serverType) {
        if (serverType == ServerType.AGM && workspaceName == null) {
            workspaceService.listWorkspacesAsync(new AbstractCachingService.DispatchCallback<Map<Integer, String>>() {
                @Override
                public void loaded(Map<Integer, String> data) {
                    workspaceName = data.get(workspaceId);
                    enabledDisable(restService.getServerTypeIfAvailable());
                }
            });
        } else {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    enabledDisable(serverType);
                }
            });
        }
    }

    private void enabledDisable(ServerType serverType) {
        if (serverType == ServerType.AGM && workspaceName != null && workspaceId != workspaceService.getWorkspaceId()) {
            ((JTextPane) getComponent()).setText(getTextValue());
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    private String getTextValue() {
        StringBuffer buf = new StringBuffer();
        buf.append("<html><body>Entity belongs to another workspace.");
        buf.append(" Switch to <a href=\"workspace\">");
        buf.append(workspaceName);
        buf.append("</a></body></html>.");
        buf.append("</body></html>.");
        return buf.toString();
    }
}
