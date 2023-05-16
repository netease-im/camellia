package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Props;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/16
 */
public class HotKeyCacheStatsRepPack extends HotKeyPackBody {

    public static final HotKeyCacheStatsRepPack INSTANCE = new HotKeyCacheStatsRepPack();

    private static final Props property = new Props();//预留一个参数

    @Override
    public void marshal(Pack pack) {
        pack.putMarshallable(property);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        unpack.popMarshallable(new Props());
    }
}
