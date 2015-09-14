// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.cfg;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XMLOutputterFactory {

    public static XMLOutputter getXMLOutputter() {
        Format format = Format.getCompactFormat().setEncoding("UTF-8");
        return new XMLOutputter(format);
    }
}
