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

package com.hp.alm.ali.idea.genesis.steps;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.genesis.checkout.CheckoutFactory;
import com.hp.alm.ali.idea.model.parser.ProviderPropertyList;
import com.hp.alm.ali.idea.genesis.WizardContext;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TargetStep extends GenesisStep implements DocumentListener {
    public TargetStep(GenesisStep previous, WizardContext ctx) {
        super(previous, ctx, Arrays.asList(ctx.targetBtn, ctx.targetLbl, ctx.targetFullLbl, ctx.targetFull));
    }

    public void _init() {
        super._init();

        Entity repo = ((EntityWrapper) ctx.repository.getSelectedItem()).getEntity();

        CheckoutFactory[] checkoutFactories = Extensions.getExtensions(CheckoutFactory.EXTENSION_POINT_NAME);
        ctx.checkout = null;
        for(CheckoutFactory cof: checkoutFactories) {
            if(repo.getProperty("repository-type").equals(cof.getType())) {
                ctx.checkout = cof.create();
                ctx.checkout.setRepository(repo);
                ctx.checkout.setBranch(((EntityWrapper) ctx.branch.getSelectedItem()).getEntity());
                break;
            }
        }

        if(ctx.checkout == null) {
            Entity branch = ((EntityWrapper) ctx.branch.getSelectedItem()).getEntity();
            StringBuffer buf = new StringBuffer();
            buf.append("Automated environment provisioning is not supported for target SCM (").append(repo.getProperty("repository-type")).append(").");
            buf.append("\nPlease checkout the source code and open project manually using following information:");
            buf.append("\n\n");
            buf.append("\nRepository location: ").append(repo.getProperty("location"));
            buf.append("\nBranch path: ").append(branch.getProperty("path"));
            if(branch.getProperty("name") != null) {
                buf.append("\nBranch name: ").append(branch.getProperty("name"));
            }
            Map<String, String> props = unmarshall((String) repo.getProperty("additional-properties"));
            if(props != null && !props.isEmpty()) {
                InputStream is = ctx.client.getForStream("scm/providers/{0}/properties", (String) repo.getProperty("repository-type"));
                ProviderPropertyList propertyList = ProviderPropertyList.create(is);
                for(String[] prop: propertyList) {
                    String value = props.get(prop[0]);
                    if(value != null) {
                        buf.append("\n").append(prop[1]).append(": ").append(value);
                    }
                }
            }
            Messages.showInfoMessage(buf.toString(), "Unsupported SCM system");
        }
    }

    public boolean isNextAvailable() {
        return ctx.targetFull.getSelectedIndex() >= 0;
    }

    private void targetChanged() {
        Entity branch = ((EntityWrapper) ctx.branch.getSelectedItem()).getEntity();
        String path = (String) branch.getProperty("path");

        int selected = ctx.targetFull.getSelectedIndex();
        ctx.targetFull.removeAllItems();
        String[] dirs = path.split("/");
        for(int i = 1; i < dirs.length; i++) {
            ctx.targetFull.addItem(ctx.targetBtn.getTextField().getText().replaceFirst("/$", "")+"/"+StringUtil.join(dirs, i, dirs.length, "/"));
        }
        if(selected < 0) {
            selected = ctx.targetFull.getItemCount() - 1;
        }
        ctx.targetFull.setSelectedIndex(selected);
    }

    private Map<String, String> unmarshall(String serialized) {
        if (serialized == null) return null;
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(serialized.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> ret = new HashMap<String, String>();
        for(String name: properties.stringPropertyNames()) {
            ret.put(name, properties.getProperty(name));
        }
        return ret;
    }

    private byte[] readFrom(InputStream is) {
        byte[] buf = new byte[16384];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int n;
            while((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } catch(IOException e) {
            return baos.toByteArray();
        }
    }

    public void insertUpdate(DocumentEvent documentEvent) {
        targetChanged();
    }

    public void removeUpdate(DocumentEvent documentEvent) {
        targetChanged();
    }

    public void changedUpdate(DocumentEvent documentEvent) {
        targetChanged();
    }
}
