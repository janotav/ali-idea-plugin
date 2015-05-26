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

package com.hp.alm.ali.idea.content.settings;

import com.hp.alm.ali.idea.cfg.AliProjectConfigurable;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.cfg.AliConfigurable;
import com.hp.alm.ali.idea.cfg.AliConfiguration;
import com.hp.alm.ali.idea.cfg.AliProjectConfiguration;
import com.hp.alm.ali.idea.cfg.AuthenticationFailed;
import com.hp.alm.ali.idea.cfg.ConfigurationListener;
import com.hp.alm.ali.rest.client.RestClient;
import com.hp.alm.ali.idea.ui.NonAdjustingCaret;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.rest.ServerTypeListener;
import com.hp.alm.ali.rest.client.exception.HttpClientErrorException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.httpclient.HttpStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsPanel extends JPanel implements ConfigurationListener, Disposable, HyperlinkListener, ServerTypeListener {

    private AliProjectConfiguration projectConf;
    private JTextPane location;
    private JTextPane domain;
    private JTextPane project;
    private JTextPane username;
    private JPanel passwordPanel;
    private JComponent connectionComponent;
    private JPanel previewAndConnection;
    private Project prj;
    private JPanel preview;
    private RestService restService;

    public SettingsPanel(final Project prj, Color bgColor) {
        this.prj = prj;
        this.projectConf = prj.getComponent(AliProjectConfiguration.class);

        previewAndConnection = new JPanel(new GridBagLayout());
        previewAndConnection.setOpaque(false);
        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = 1;
        c2.gridwidth = 2;
        c2.weighty = 1;
        c2.fill = GridBagConstraints.VERTICAL;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        previewAndConnection.add(filler, c2);

        passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.setBackground(bgColor);
        JLabel label = new JLabel("Password");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        passwordPanel.add(label);
        final JPasswordField password = new JPasswordField(24);
        passwordPanel.add(password);
        JButton connect = new JButton("Login");
        passwordPanel.add(connect);
        final JLabel message = new JLabel();
        passwordPanel.add(message);
        ActionListener connectionAction = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    checkConnection(projectConf.getLocation(), projectConf.getDomain(), projectConf.getProject(), projectConf.getUsername(), password.getText());
                } catch (AuthenticationFailed e) {
                    message.setText(e.getMessage());
                    return;
                }
                projectConf.ALM_PASSWORD = password.getText();
                projectConf.fireChanged();
            }
        };
        password.addActionListener(connectionAction);
        connect.addActionListener(connectionAction);

        restService = prj.getComponent(RestService.class);
        restService.addServerTypeListener(this);

        location = createTextPane(bgColor);
        domain = createTextPane(bgColor);
        project = createTextPane(bgColor);
        username = createTextPane(bgColor);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JTextPane textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
        textPane.setText("<html><body>HP ALM integration can be configured on <a href=\"ide\">IDE</a> and overridden on <a href=\"project\">project</a> level.</body></html>");
        textPane.setEditable(false);
        textPane.addHyperlinkListener(this);
        textPane.setBackground(bgColor);
        textPane.setCaret(new NonAdjustingCaret());
        panel.add(textPane, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(bgColor);
        content.add(panel, BorderLayout.NORTH);
        content.add(previewAndConnection, BorderLayout.WEST);

        preview = new JPanel(new GridBagLayout()) {
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                // make enough room for the connection status message
                dim.width = Math.max(dim.width, 300);
                return dim;
            }

            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        connectedTo(restService.getServerTypeIfAvailable());
        preview.setBackground(bgColor);

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        preview.add(location, c);
        c.gridwidth = 1;
        c.gridy++;
        preview.add(domain, c);
        c.gridy++;
        preview.add(project, c);
        c.gridy++;
        preview.add(username, c);
        c.gridx++;
        c.gridy--;
        c.gridheight = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.SOUTHEAST;
        final LinkLabel reload = new LinkLabel("Reload", IconLoader.getIcon("/actions/sync.png"));
        reload.setListener(new LinkListener() {
            public void linkSelected(LinkLabel linkLabel, Object o) {
                projectConf.fireChanged();
            }
        }, null);
        preview.add(reload, c);

        JPanel previewNorth = new JPanel(new BorderLayout());
        previewNorth.setBackground(bgColor);
        previewNorth.add(preview, BorderLayout.NORTH);

        addToGridBagPanel(0, 0, previewAndConnection, previewNorth);

        setBackground(bgColor);
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        onChanged();
        ApplicationManager.getApplication().getComponent(AliConfiguration.class).addListener(this);
        projectConf.addListener(this);
    }

    private void addToGridBagPanel(int x, int y, JPanel gridBagPanel, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.NONE;
        gridBagPanel.add(comp, c);
    }

    private void checkConnection(String location, String domain, String project, String username, String password) throws AuthenticationFailed {
        RestClient restClient = RestService.createRestClient(location, domain, project, username, password, RestClient.SessionStrategy.AUTO_LOGIN);
        try {
            restClient.getForString("defects?query={0}", EntityQuery.encode("{id[0]}"));
            RestService.logout(restClient);
        } catch(HttpClientErrorException e) {
            if(e.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED) {
                throw new AuthenticationFailed();
            } else {
                throw new RuntimeException("Failed to connect to HP ALM");
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to connect to HP ALM");
        }
    }

    private void setCaption(String caption) {
        preview.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 10, 10, 10), BorderFactory.createTitledBorder(caption)), new EmptyBorder(10, 10, 10, 10)));
    }

    public void onChanged() {
        location.setText("Location: " + projectConf.getLocation());
        domain.setText("Domain: " + projectConf.getDomain());
        project.setText("Project: " + projectConf.getProject());
        username.setText("Username: " + projectConf.getUsername());
    }

    private JTextPane createTextPane(Color bgColor) {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setBackground(bgColor);
        pane.setCaret(new NonAdjustingCaret());
        return pane;
    }

    public void dispose() {
        projectConf.removeListener(this);
    }

    public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
        if(hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (hyperlinkEvent.getDescription().equals("project")) {
                ShowSettingsUtil.getInstance().showSettingsDialog(prj, AliProjectConfigurable.DISPLAY_NAME);
            } else {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, AliConfigurable.DISPLAY_NAME);
            }
        }
    }

    public void connectedTo(final ServerType serverType) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                setCaption("Current Settings: "+serverType);
                if(serverType == ServerType.NEEDS_PASSWORD) {
                    add(passwordPanel, BorderLayout.NORTH);
                    revalidate();
                    repaint();
                } else {
                    remove(passwordPanel);
                }
                if (connectionComponent != null) {
                    previewAndConnection.remove(connectionComponent);
                    connectionComponent = null;
                }
                if (serverType.isConnected()) {
                    connectionComponent = prj.getComponent(serverType.getClazz()).getConnectionComponent();
                    if (connectionComponent != null) {
                        addToGridBagPanel(1, 0, previewAndConnection, connectionComponent);
                    }
                }
            }
        });
    }
}
