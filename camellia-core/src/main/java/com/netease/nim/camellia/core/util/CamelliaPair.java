package com.netease.nim.camellia.core.util;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/4/10.
 */
public class CamelliaPair<T, W> {
    private T first;
    private W second;

    public CamelliaPair() {
    }

    public CamelliaPair(T first, W second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public W getSecond() {
        return second;
    }

    public void setSecond(W second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CamelliaPair<?, ?> camelliaPair = (CamelliaPair<?, ?>) o;
        return Objects.equals(first, camelliaPair.first) &&
                Objects.equals(second, camelliaPair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
