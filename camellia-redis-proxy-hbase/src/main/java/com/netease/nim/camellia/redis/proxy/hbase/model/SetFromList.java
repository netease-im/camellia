package com.netease.nim.camellia.redis.proxy.hbase.model;


import java.io.Serializable;
import java.util.*;

public class SetFromList<E> extends AbstractSet<E> implements Serializable {
    private static final long serialVersionUID = -2850347066962734052L;
    private final transient List<E> list;

    private SetFromList(List<E> list) {
        if (list == null) {
            throw new NullPointerException("list");
        }
        this.list = list;
    }

    public void clear() {
        list.clear();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean add(E e) {
        return !contains(e) && list.add(e);
    }

    public Iterator<E> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Set)) {
            return false;
        }

        Collection<?> c = (Collection<?>) o;
        if (c.size() != size()) {
            return false;
        }

        return containsAll(c);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public static <E> SetFromList<E> of(List<E> list) {
        return new SetFromList<>(list);
    }
}
