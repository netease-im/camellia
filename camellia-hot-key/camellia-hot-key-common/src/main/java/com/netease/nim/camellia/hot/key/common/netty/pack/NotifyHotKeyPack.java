package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
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
public class NotifyHotKeyPack extends HotKeyPackBody {

    private static enum Tag {
        key(1),
        action(2),
        expireMillis(3),
        ;

        private final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    private List<HotKey> list;

    public NotifyHotKeyPack(List<HotKey> list) {
        this.list = list;
    }

    public NotifyHotKeyPack() {
    }

    public List<HotKey> getList() {
        return list;
    }

    @Override
    public void marshal(Pack pack) {
        ArrayMable<Property> arrayMable = new ArrayMable<>(Property.class);
        for (HotKey hotKey : list) {
            Property property = new Property();
            property.put(Tag.key.value, hotKey.getKey());
            property.putInteger(Tag.action.value, hotKey.getAction().getValue());
            property.putLong(Tag.expireMillis.value, hotKey.getExpireMillis());
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
            HotKey hotKey = new HotKey();
            hotKey.setKey(property.get(Tag.key.value));
            hotKey.setAction(KeyAction.getByValue(property.getInteger(Tag.action.value)));
            hotKey.setExpireMillis(property.getLong(Tag.expireMillis.value));
            list.add(hotKey);
        }
    }
}
