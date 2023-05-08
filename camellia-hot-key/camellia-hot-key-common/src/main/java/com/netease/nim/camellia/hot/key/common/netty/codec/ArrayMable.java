package com.netease.nim.camellia.hot.key.common.netty.codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayMable<T extends Marshallable> implements Marshallable {

    private static final int MAX_SIZE = 100000;

    public List<T> list;
    public Class<? extends Marshallable> clazz;

    public ArrayMable(Class<? extends Marshallable> clazz) {
        this.clazz = clazz;
        list = new ArrayList<>();
    }

    public ArrayMable(int size) {
        list = new ArrayList<>(size);
    }

    public ArrayMable(List<T> list, Class<? extends Marshallable> clazz) {
        this.clazz = clazz;
        this.list = list;
    }

    public ArrayMable(Collection<T> list, Class<? extends Marshallable> clazz) {
        this.clazz = clazz;
        this.list = new ArrayList<>(list);
    }

    public void add(T o) {
        this.list.add(o);
    }

    public void addAll(Collection<T> list) {
        this.list.addAll(list);
    }
    
    public void add(int index, T o) {
        this.list.add(index, o);
    }

    public int size() {
        return list.size();
    }

    public void clear(){
        list.clear();
    }

    @Override
    public void marshal(Pack pack) {
        pack.putVarUint(list.size());
        for (T t : list) {
            pack.putMarshallable(t);
        }
    }

    @Override
    public void unmarshal(Unpack unpack) {
        int len = unpack.popVarUint();
        if (len > MAX_SIZE) {
            throw new UnpackException("ArrayMable size illegal");
        }
        list = new ArrayList<>(len);
        for (int i = 0; i < len; ++i) {
            try {
                T t = (T) clazz.getConstructor().newInstance();
                unpack.popMarshallable(t);
                list.add(t);
            } catch (Exception e) {
                throw new UnpackException(e);
            }
        }
    }

}
