package com.hp.alm.ali.utils;

import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static com.hp.alm.ali.utils.StringUtils.joinWithSeparator;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StringUtilsTest {

    @Test
    public void testExpand() throws Exception {
        String expand = StringUtils.expand("a{a{XXX}}{XXX}}", of("XXX", "777"));
        assertThat(expand, is("a{a777}777}"));
        expand = StringUtils.expand("a{a{XXX}}{XXX}{YYY}}", of("XXX", "777", "YYY", "888"));
        assertThat(expand, is("a{a777}777888}"));
    }


    @Test
    public void testJoin() throws Exception {
        assertThat(joinWithSeparator("/", "aaa", "bbb"), is("aaa/bbb"));
        assertThat(joinWithSeparator("/", "a", "b"), is("a/b"));
        assertThat(joinWithSeparator("/", "a/", "/b"), is("a/b"));
        assertThat(joinWithSeparator("/", "a/", "b/"), is("a/b/"));
        assertThat(joinWithSeparator("/"), is(""));
        assertThat(joinWithSeparator("/", "a"), is("a"));
        assertThat(joinWithSeparator("/", "/a/"), is("/a/"));
    }

}
