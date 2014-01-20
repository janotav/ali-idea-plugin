package com.hp.alm.ali.idea.genesis.steps;

import com.hp.alm.ali.idea.cfg.AliConfigurable;
import com.hp.alm.ali.idea.genesis.WizardContext;
import com.hp.alm.ali.idea.rest.ServerType;

import javax.swing.*;
import java.util.List;

public class ServerTypeAwareStep extends GenesisStep {

    private ServerType serverType;

    public ServerTypeAwareStep(GenesisStep previous, WizardContext ctx, List<? extends JComponent> myControls) {
        super(previous, ctx, myControls);
    }


    public void _init() {
        super._init();
        serverType = AliConfigurable.getServerType(ctx.location.getText(), (String) ctx.domain.getSelectedItem(),
                (String) ctx.project.getSelectedItem(), ctx.username.getText(), ctx.password.getText());
    }

    protected ServerType getServerType() {
        return serverType;
    }

    protected String getScmRepositoriesTemplatePref() {
        if (getServerType() == ServerType.ALM115 || getServerType() == ServerType.ALM12) {
            return "scm-repositorys";
        }
        return "scm-repositories";
    }

    protected String getScmBranchTemplatePref() {
        if (getServerType() == ServerType.ALM115 || getServerType() == ServerType.ALM12) {
            return "scm-branchs";
        }
        return "scm-branches";
    }
}
