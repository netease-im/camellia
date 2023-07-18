package com.netease.nim.camellia.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionSplitUtil {

    public static <T> List<List<T>> split(Collection<T> collection, int splitSize) {
        if (collection == null) return null;
        if (collection.isEmpty()) return new ArrayList<>();
        List<List<T>> res = new ArrayList<>(collection.size() / splitSize + 1);
        if (collection.size() < splitSize) {
            res.add(new ArrayList<>(collection));
        } else {
            List<T> tmp = new ArrayList<>(splitSize);
            for (T t : collection) {
                tmp.add(t);
                if (tmp.size() == splitSize) {
                    res.add(tmp);
                    tmp = new ArrayList<>(splitSize);
                }
            }
            if (!tmp.isEmpty()) {
                res.add(tmp);
            }
        }
        return res;
    }
}
