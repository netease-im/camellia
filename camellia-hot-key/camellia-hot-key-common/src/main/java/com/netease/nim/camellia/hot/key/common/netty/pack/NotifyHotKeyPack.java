package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.codec.ArrayMable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class NotifyHotKeyPack extends HotKeyPackBody {

    private static enum Tag {
        namespace(1),
        key(2),
        action(3),
        expireMillis(4),
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
        ArrayMable<Props> arrayMable = new ArrayMable<>(Props.class);
        for (HotKey hotKey : list) {
            Props props = new Props();
            props.put(Tag.namespace.value, hotKey.getNamespace());
            props.put(Tag.key.value, hotKey.getKey());
            props.putInteger(Tag.action.value, hotKey.getAction().getValue());
            if (hotKey.getExpireMillis() != null) {
                props.putLong(Tag.expireMillis.value, hotKey.getExpireMillis());
            }
            arrayMable.add(props);
        }
        pack.putMarshallable(arrayMable);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        ArrayMable<Props> arrayMable = new ArrayMable<>(Props.class);
        unpack.popMarshallable(arrayMable);
        list = new ArrayList<>();
        for (Props property : arrayMable.list) {
            HotKey hotKey = new HotKey();
            hotKey.setNamespace(property.get(Tag.namespace.value));
            hotKey.setKey(property.get(Tag.key.value));
            hotKey.setAction(KeyAction.getByValue(property.getInteger(Tag.action.value)));
            if (property.containsKey(Tag.expireMillis.value)) {
                hotKey.setExpireMillis(property.getLong(Tag.expireMillis.value));
            }
            list.add(hotKey);
        }
    }
}
