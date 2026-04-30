package com.netease.nim.camellia.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2026/4/29
 */
public class CollectionSplitUtilTest {

    @Test
    public void shouldReturnNullForNullCollection() {
        Assert.assertNull(CollectionSplitUtil.split(null, 10));
    }

    @Test
    public void shouldReturnEmptyListForEmptyCollection() {
        List<List<Integer>> result = CollectionSplitUtil.split(Collections.emptyList(), 10);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldKeepSingleChunkWhenCollectionIsSmallerThanSplitSize() {
        List<Integer> source = Arrays.asList(1, 2);

        List<List<Integer>> result = CollectionSplitUtil.split(source, 3);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(source, result.get(0));
        Assert.assertNotSame(source, result.get(0));
    }

    @Test
    public void shouldSplitCollectionIntoOrderedChunks() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> result = CollectionSplitUtil.split(source, 2);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(Arrays.asList(1, 2), result.get(0));
        Assert.assertEquals(Arrays.asList(3, 4), result.get(1));
        Assert.assertEquals(Collections.singletonList(5), result.get(2));
    }

    @Test
    public void shouldNotAppendEmptyChunkForExactMultiple() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4);

        List<List<Integer>> result = CollectionSplitUtil.split(source, 2);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Arrays.asList(1, 2), result.get(0));
        Assert.assertEquals(Arrays.asList(3, 4), result.get(1));
    }
}
