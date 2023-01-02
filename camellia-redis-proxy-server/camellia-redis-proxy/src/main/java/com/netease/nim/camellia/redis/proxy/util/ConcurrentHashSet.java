package com.netease.nim.camellia.redis.proxy.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/9/30
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
    private final ConcurrentHashMap<E, Boolean> map = new ConcurrentHashMap<>();

    public ConcurrentHashSet() {
    }

    public int size() {
        return this.map.size();
    }

    public boolean contains(Object o) {
        return this.map.containsKey(o);
    }

    public Iterator<E> iterator() {
        return this.map.keySet().iterator();
    }

    public boolean add(E o) {
        return this.map.putIfAbsent(o, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return this.map.remove(o) != null;
    }

    public void clear() {
        this.map.clear();
    }
}
