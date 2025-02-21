package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.cluster.SlotSplitUtils;
import com.netease.nim.camellia.tools.utils.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/2/21
 */
public class TestSlotSplit {

    @Test
    public void test1() {
        List<Integer> list = new ArrayList<>();
        for (int i=0; i<=100; i++) {
            list.add(i);
        }
        List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(list);
        Assert.assertEquals(pairs.size(), 1);
        Pair<Integer, Integer> pair = pairs.get(0);
        Assert.assertEquals(String.valueOf(pair.getFirst()), "0");
        Assert.assertEquals(String.valueOf(pair.getSecond()), "100");
    }

    @Test
    public void test2() {
        List<Integer> list = new ArrayList<>();
        for (int i=0; i<=100; i++) {
            list.add(i);
        }
        for (int i=200;i<=300;i++) {
            list.add(i);
        }
        List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(list);
        Assert.assertEquals(pairs.size(), 2);
        {
            Pair<Integer, Integer> pair = pairs.get(0);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "0");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "100");
        }
        {
            Pair<Integer, Integer> pair = pairs.get(1);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "200");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "300");
        }
    }

    @Test
    public void test3() {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(list);
        Assert.assertEquals(pairs.size(), 1);
        Pair<Integer, Integer> pair = pairs.get(0);
        Assert.assertEquals(String.valueOf(pair.getFirst()), "0");
        Assert.assertEquals(String.valueOf(pair.getSecond()), "0");
    }

    @Test
    public void test4() {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        for (int i=100; i<=200; i++) {
            list.add(i);
        }
        List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(list);
        Assert.assertEquals(2, pairs.size());
        {
            Pair<Integer, Integer> pair = pairs.get(0);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "0");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "0");
        }
        {
            Pair<Integer, Integer> pair = pairs.get(1);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "100");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "200");
        }
    }

    @Test
    public void test5() {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(2);
        for (int i=100; i<=200; i++) {
            list.add(i);
        }
        list.add(202);
        List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(list);
        Assert.assertEquals(4, pairs.size());
        {
            Pair<Integer, Integer> pair = pairs.get(0);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "0");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "0");
        }
        {
            Pair<Integer, Integer> pair = pairs.get(1);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "2");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "2");
        }
        {
            Pair<Integer, Integer> pair = pairs.get(2);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "100");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "200");
        }
        {
            Pair<Integer, Integer> pair = pairs.get(3);
            Assert.assertEquals(String.valueOf(pair.getFirst()), "202");
            Assert.assertEquals(String.valueOf(pair.getSecond()), "202");
        }
    }
}
