package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Props;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/8
 */
public class NotifyHotKeyRepPack extends HotKeyPackBody {

    public static final NotifyHotKeyRepPack INSTANCE = new NotifyHotKeyRepPack();

    private static final Props props = new Props();//预留一个参数

    @Override
    public void marshal(Pack pack) {
        pack.putMarshallable(props);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        unpack.popMarshallable(new Props());
    }
}
