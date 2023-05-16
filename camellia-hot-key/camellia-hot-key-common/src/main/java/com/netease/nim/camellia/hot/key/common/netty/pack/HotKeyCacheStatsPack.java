package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.ArrayMable;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Property;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/16
 */
public class HotKeyCacheStatsPack extends HotKeyPackBody {

    private static enum Tag {
        namespace(1),
        key(2),
        hitCount(3),
        ;

        private final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    private List<HotKeyCacheStats> statsList;

    public HotKeyCacheStatsPack(List<HotKeyCacheStats> statsList) {
        this.statsList = statsList;
    }

    public HotKeyCacheStatsPack() {
    }

    public List<HotKeyCacheStats> getStatsList() {
        return statsList;
    }

    @Override
    public void marshal(Pack pack) {
        ArrayMable<Property> arrayMable = new ArrayMable<>(Property.class);
        for (HotKeyCacheStats stats : statsList) {
            Property property = new Property();
            property.put(Tag.namespace.value, stats.getNamespace());
            property.put(Tag.key.value, stats.getKey());
            property.putLong(Tag.hitCount.value, stats.getHitCount());
            arrayMable.add(property);
        }
        pack.putMarshallable(arrayMable);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        ArrayMable<Property> arrayMable = new ArrayMable<>(Property.class);
        unpack.popMarshallable(arrayMable);
        statsList = new ArrayList<>();
        for (Property property : arrayMable.list) {
            HotKeyCacheStats stats = new HotKeyCacheStats();
            stats.setNamespace(property.get(Tag.namespace.value));
            stats.setKey(property.get(Tag.key.value));
            stats.setHitCount(property.getLong(Tag.hitCount.value));
            statsList.add(stats);
        }
    }
}
