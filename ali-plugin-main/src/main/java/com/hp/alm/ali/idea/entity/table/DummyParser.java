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

package com.hp.alm.ali.idea.entity.table;

import net.coderazzi.filters.IParser;

import javax.swing.*;
import java.text.ParseException;

/**
 * This parser does nothing (accepts all rows). We always perform the filter in the ALM database.
 */
public class DummyParser implements IParser {

    public RowFilter parseText(String expression) throws ParseException {
        return new AnyRowFilter();
    }

    public InstantFilter parseInstantText(String expression) throws ParseException {
        InstantFilter filter = new InstantFilter();
        filter.expression = expression;
        filter.filter = parseText(expression);
        return filter;
    }

    public String escape(String s) {
        return s;
    }
}
