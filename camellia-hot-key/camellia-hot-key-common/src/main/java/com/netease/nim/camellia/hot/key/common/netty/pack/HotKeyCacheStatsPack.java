package com.netease.nim.camellia.hot.key.common.netty.pack;


import com.netease.nim.camellia.codec.ArrayMable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.Unpack;

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
        ArrayMable<Props> arrayMable = new ArrayMable<>(Props.class);
        for (HotKeyCacheStats stats : statsList) {
            Props props = new Props();
            props.put(Tag.namespace.value, stats.getNamespace());
            props.put(Tag.key.value, stats.getKey());
            props.putLong(Tag.hitCount.value, stats.getHitCount());
            arrayMable.add(props);
        }
        pack.putMarshallable(arrayMable);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        ArrayMable<Props> arrayMable = new ArrayMable<>(Props.class);
        unpack.popMarshallable(arrayMable);
        statsList = new ArrayList<>();
        for (Props props : arrayMable.list) {
            HotKeyCacheStats stats = new HotKeyCacheStats();
            stats.setNamespace(props.get(Tag.namespace.value));
            stats.setKey(props.get(Tag.key.value));
            stats.setHitCount(props.getLong(Tag.hitCount.value));
            statsList.add(stats);
        }
    }
}
