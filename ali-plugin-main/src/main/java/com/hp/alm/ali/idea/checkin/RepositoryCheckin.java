// (C) Copyright 2003-2014 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.checkin;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vcs.CheckinProjectPanel;

public interface RepositoryCheckin {

    ExtensionPointName<RepositoryCheckin> EXTENSION_POINT_NAME = ExtensionPointName.create("com.hp.alm.ali.repositoryCheckin");

    boolean isMergingCheckin(CheckinProjectPanel checkinProjectPanel);

}
