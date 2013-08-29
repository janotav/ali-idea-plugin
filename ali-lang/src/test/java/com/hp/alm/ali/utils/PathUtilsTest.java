package com.hp.alm.ali.utils;

import org.junit.Test;

import static com.hp.alm.ali.utils.PathUtils.pathJoin;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PathUtilsTest {

    @Test
    public void testJoin() throws Exception {
        assertThat(pathJoin("/", "aaa", "bbb"), is("aaa/bbb"));
        assertThat(pathJoin("/", "a", "b"), is("a/b"));
        assertThat(pathJoin("/", "a/", "/b"), is("a/b"));
        assertThat(pathJoin("/", "a/", "b/"), is("a/b/"));
        assertThat(pathJoin("/"), is(""));
        assertThat(pathJoin("/", "a"), is("a"));
        assertThat(pathJoin("/", "/a/"), is("/a/"));
    }
}
