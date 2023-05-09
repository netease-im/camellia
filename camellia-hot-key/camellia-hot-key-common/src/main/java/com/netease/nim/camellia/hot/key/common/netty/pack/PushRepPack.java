package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Property;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/8
 */
public class PushRepPack extends HotKeyPackBody {

    public static final PushRepPack INSTANCE = new PushRepPack();

    private static final Property property = new Property();//预留一个参数

    @Override
    public void marshal(Pack pack) {
        pack.putMarshallable(property);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        unpack.popMarshallable(new Property());
    }
}
