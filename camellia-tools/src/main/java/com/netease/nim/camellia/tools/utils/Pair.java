package com.netease.nim.camellia.tools.utils;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/6/17
 */
public class Pair<T, W> {
    private T first;
    private W second;

    public Pair() {
    }

    public Pair(T first, W second) {
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
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) &&
                Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
