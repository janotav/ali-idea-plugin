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

package com.hp.alm.ali.idea.translate;

import com.hp.alm.ali.ServerVersion;
import com.hp.alm.ali.idea.IntellijTest;
import com.hp.alm.ali.idea.RestInvocations;
import com.hp.alm.ali.idea.model.Field;
import com.hp.alm.ali.idea.model.type.BacklogBlockedType;
import com.hp.alm.ali.idea.model.type.BuildDurationType;
import com.hp.alm.ali.idea.model.type.BuildStatusType;
import com.hp.alm.ali.idea.model.type.Context;
import com.hp.alm.ali.idea.model.type.ContextAware;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.rest.ServerType;
import com.hp.alm.ali.idea.translate.filter.ExpressionResolver;
import com.hp.alm.ali.idea.translate.filter.FilterResolver;
import com.hp.alm.ali.idea.translate.filter.MultipleItemsResolver;
import com.hp.alm.ali.idea.translate.filter.TranslatorAsync;
import com.hp.alm.ali.idea.translate.filter.TranslatorSync;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;

public class TranslateServiceTest extends IntellijTest {

    private TranslateService translateService;
    private RestService restService;

    @Before
    public void preClean() {
        translateService = getComponent(TranslateService.class);
        translateService.connectedTo(ServerType.AGM); // clear cache
        restService = getComponent(RestService.class);
    }

    public TranslateServiceTest() {
        super(ServerVersion.AGM);
    }

    @Test
    public void testTranslateAsync() {
        handler.async();
        String value = translateService.translateAsync(new TranslatorAsync(), "VALUE", false, new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertTrue("Callback outside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("value", value);
                    }
                });
            }
        });
        Assert.assertEquals("Loading...", value);
    }

    @Test
    public void testTranslateAsync_sync() {
        String value = translateService.translateAsync(new TranslatorSync(), "VALUE", false, new ValueCallback() {
            @Override
            public void value(String value) {
                Assert.fail("Not expected");
            }
        });
        Assert.assertEquals("value", value);
    }

    @Test
    public void testTranslateAsync_syncCallback() {
        handler.async();
        String value = translateService.translateAsync(new TranslatorSync(), "VALUE", true, new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("value", value);
                    }
                });
            }
        });
        Assert.assertEquals("value", value);
    }

    @Test
    public void testTranslateAsync_empty() {
        handler.async();
        String value = translateService.translateAsync(new TranslatorAsync(), "", true, new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("", value);
                    }
                });
            }
        });
        Assert.assertEquals("", value);
    }

    @Test
    public void testTranslateAsync_context() {
        handler.async();
        String value = translateService.translateAsync(new ContextTranslator(), "", true, new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertTrue("Callback outside dispatch thread", SwingUtilities.isEventDispatchThread());
                        Assert.assertEquals("foo", value);
                    }
                });
            }
        });
        Assert.assertEquals("Loading...", value);
    }

    private void doTestGetReferenceTranslator(boolean loadMetadata) {
        if(loadMetadata) {
            RestInvocations.loadMetadata(handler, "defect");
            RestInvocations.loadMetadata(handler, "release-backlog-item");
        }

        handler.addRequest(false, "GET", "/qcbin/rest/domains/domain/projects/project/defects?fields=planned-closing-ver,has-change,reproducible,changeset,has-others-linkage,priority,description,dev-comments,release-backlog-item.story-points,release-backlog-item.team-id,status,release-backlog-item.kan-status-duration,release-backlog-item.no-of-sons,closing-date,release-backlog-item.kanban-parent-status-id,detected-in-rel,estimated-fix-time,release-backlog-item.remaining,release-backlog-item.entity-id,actual-fix-time,release-backlog-item.feature-id,release-backlog-item.linked-entities-info,user-04,user-03,user-02,user-01,subject,build-closed,in-bucket,id,release-backlog-item.status,release-backlog-item.release-id,release-backlog-item.entity-name,name,has-linkage,release-backlog-item.owner,release-backlog-item.estimated,release-backlog-item.entity-type,creation-time,release-backlog-item.rank,closing-version,build-detected,release-backlog-item.theme-id,detection-version,release-backlog-item.product-id,last-modified,release-backlog-item.blocked,watch-id,detected-in-rcyc,release-backlog-item.kanban-status-id,severity,attachment,release-backlog-item.kan-parent-duration,release-backlog-item.invested,release-backlog-item.sprint-id,extended-reference,release-backlog-item.watch-id,detected-by,fixed-on-date,release-backlog-item.id&query={id[86]}&order-by={}", 200)
                .content("entityServiceTest_entity.xml");

        Translator translator = translateService.getReferenceTranslator("defect");
        handler.async();
        String value = translator.translate("86", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("somewhat", value);
                    }
                });
            }
        });
        Assert.assertNull(value);
    }

    @Test
    public void testGetReferenceTranslator() {
        doTestGetReferenceTranslator(true);
    }

    @Test
    public void testGetReferenceTranslator_caches() throws Throwable {
        doTestGetReferenceTranslator(true);
        handler.checkpoint();

        // should be served from cache
        Translator translator = translateService.getReferenceTranslator("defect");
        String value = translator.translate("86", new ValueCallback() {
            @Override
            public void value(String value) {
                Assert.fail("Not expected");
            }
        });
        Assert.assertEquals("somewhat", value);
    }

    @Test
    public void testGetReferenceTranslator_invalidatesCache() throws Throwable {
        doTestGetReferenceTranslator(true);
        handler.checkpoint();

        translateService.connectedTo(ServerType.AGM);

        // should query again (no metadata though)
        doTestGetReferenceTranslator(false);
    }

    @Test
    public void testListensForServerTypeEvent() {
        Assert.assertTrue(restService._isRegistered(translateService));
    }

    @Test
    public void testGetTranslator_reference() {
        Field field = new Field("build-detected", "Detected in build");
        field.setReferencedType("build-instance");
        Translator translator = translateService.getTranslator(field);
        Assert.assertTrue(translator instanceof EntityReferenceTranslator);
        Assert.assertEquals("build-instance", ((EntityReferenceTranslator) translator).getTargetType());
    }

    @Test
    public void testGetTranslator_type() {
        Field field = new Field("duration", "Duration");
        field.setClazz(BuildDurationType.class);
        Translator translator = translateService.getTranslator(field);
        Assert.assertTrue(translator instanceof BuildDurationType);
    }

    @Test
    public void testGetTranslator_none() {
        Field field = new Field("name", "Name");
        field.setClazz(String.class);
        Translator translator = translateService.getTranslator(field);
        Assert.assertNull(translator);
    }

    @Test
    public void testGetTranslator_typeNone() {
        Field field = new Field("status", "Status");
        field.setClazz(BuildStatusType.class);
        Translator translator = translateService.getTranslator(field);
        Assert.assertNull(translator);
    }

    @Test
    public void testGetFilterResolver_reference() {
        Field field = new Field("build-detected", "Detected in build");
        field.setReferencedType("build-instance");
        FilterResolver resolver = translateService.getFilterResolver(field);
        Assert.assertTrue(resolver instanceof ExpressionResolver);
        Assert.assertEquals("build-instance", ((EntityReferenceTranslator)((ExpressionResolver) resolver).getTranslator()).getTargetType());
    }

    @Test
    public void testGetFilterResolver_type() {
        Field field = new Field("duration", "Duration");
        field.setClazz(BuildDurationType.class);
        FilterResolver resolver = translateService.getFilterResolver(field);
        Assert.assertTrue(resolver instanceof BuildDurationType);
    }

    @Test
    public void testGetFilterResolver_none() {
        Field field = new Field("name", "Name");
        field.setClazz(String.class);
        FilterResolver resolver = translateService.getFilterResolver(field);
        Assert.assertNull(resolver);
    }

    @Test
    public void testGetFilterResolver_list() {
        Field field = new Field("severity", "Severity");
        field.setListId(299);
        FilterResolver resolver = translateService.getFilterResolver(field);
        Assert.assertTrue(resolver instanceof MultipleItemsResolver);
    }

    @Test
    public void testConvertQueryModelToView() {
        Field field = new Field("status", "Status");
        field.setClazz(BuildStatusType.class);
        handler.async();
        String value = translateService.convertQueryModelToView(field, "Success;\"\"", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("Success;(no value)", value);
                    }
                });
            }
        });
        Assert.assertEquals("Success;(no value)", value);
    }

    @Test
    public void testConvertQueryModelToView_none() {
        Field field = new Field("name", "Name");
        field.setClazz(String.class);
        handler.async();
        String value = translateService.convertQueryModelToView(field, "Success;\"\"", new ValueCallback() {
            @Override
            public void value(final String value) {
                handler.done(new Runnable() {
                    @Override
                    public void run() {
                        Assert.assertEquals("Success;\"\"", value);
                    }
                });
            }
        });
        Assert.assertEquals("Success;\"\"", value);
    }

    @Test
    public void testConvertQueryModelToREST() {
        Field field = new Field("blocked", "Blocked");
        field.setClazz(BacklogBlockedType.class);
        String value = translateService.convertQueryModelToREST(field, "blocked");
        Assert.assertEquals("'**'", value);
    }

    @Test
    public void testConvertQueryModelToREST_none() {
        Field field = new Field("name", "Name");
        field.setClazz(String.class);
        String value = translateService.convertQueryModelToREST(field, "blocked");
        Assert.assertEquals("blocked", value);
    }

    @Test
    public void isTranslated() {
        Field field = new Field("duration", "Duration");
        field.setClazz(BuildDurationType.class);
        Assert.assertTrue(translateService.isTranslated(field));
    }

    @Test
    public void isTranslated_negative() {
        Field field = new Field("name", "Name");
        field.setClazz(String.class);
        Assert.assertFalse(translateService.isTranslated(field));
    }

    private static class ContextTranslator implements Translator, ContextAware {

        @Override
        public void setContext(Context context) {
        }

        @Override
        public String translate(String value, final ValueCallback callback) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    callback.value("foo");
                }
            });
            return null;
        }
    }
}
