package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKeyCounter;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.netty.codec.ArrayMable;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Property;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class PushPack extends HotKeyPackBody {

    private static enum Tag {
        key(1),
        action(1),
        count(1),
        ;

        private final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    private List<HotKeyCounter> list;

    public PushPack(List<HotKeyCounter> list) {
        this.list = list;
    }

    public PushPack() {
    }

    public List<HotKeyCounter> getList() {
        return list;
    }

    @Override
    public void marshal(Pack pack) {
        ArrayMable<Property> arrayMable = new ArrayMable<>(Property.class);
        for (HotKeyCounter counter : list) {
            Property property = new Property();
            property.put(Tag.key.value, counter.getKey());
            property.putInteger(Tag.action.value, counter.getAction().getValue());
            property.putLong(Tag.key.value, counter.getCount());
            arrayMable.add(property);
        }
        pack.putMarshallable(arrayMable);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        ArrayMable<Property> arrayMable = new ArrayMable<>(Property.class);
        unpack.popMarshallable(arrayMable);
        list = new ArrayList<>();
        for (Property property : arrayMable.list) {
            HotKeyCounter counter = new HotKeyCounter();
            counter.setKey(property.get(Tag.key.value));
            counter.setAction(KeyAction.getByValue(property.getInteger(Tag.action.value)));
            counter.setCount(property.getLong(Tag.count.value));
            list.add(counter);
        }
    }
}
