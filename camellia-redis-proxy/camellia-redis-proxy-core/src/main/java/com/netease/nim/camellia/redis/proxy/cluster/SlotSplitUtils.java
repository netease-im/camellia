package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.tools.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/2/21
 */
public class SlotSplitUtils {

    public static List<Pair<Integer, Integer>> splitSlots(List<Integer> slots) {
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        int start = -1;
        int stop = -1;
        for (Integer slot : slots) {
            if (start == -1) {
                start = slot;
                continue;
            }
            if (stop == -1 && start >=0 && slot == start + 1) {
                stop = slot;
                continue;
            }
            if (slot == stop + 1) {
                stop = slot;
                continue;
            }
            if (stop == -1) {
                list.add(new Pair<>(start, start));
            } else {
                list.add(new Pair<>(start, stop));
            }
            start = slot;
            stop = -1;
        }
        if (start != -1 && stop == -1) {
            list.add(new Pair<>(start, start));
        }
        if (start != -1 && stop != -1) {
            list.add(new Pair<>(start, stop));
        }
        return list;
    }
}
