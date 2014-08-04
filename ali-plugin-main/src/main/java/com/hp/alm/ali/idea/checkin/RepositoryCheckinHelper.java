// (C) Copyright 2003-2014 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.checkin;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.vcs.CheckinProjectPanel;

public class RepositoryCheckinHelper {

    public static boolean isMergingCommit(CheckinProjectPanel checkinProjectPanel) {
        RepositoryCheckin[] checkins = Extensions.getExtensions(RepositoryCheckin.EXTENSION_POINT_NAME);
        for (RepositoryCheckin checkin: checkins) {
            if (checkin.isMergingCheckin(checkinProjectPanel)) {
                return true;
            }
        }
        return false;
    }
}
