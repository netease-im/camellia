package com.netease.nim.camellia.hot.key.common.netty.pack;


import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/8
 */
public class NotifyHotKeyConfigRepPack extends HotKeyPackBody {

    public static final NotifyHotKeyConfigRepPack INSTANCE = new NotifyHotKeyConfigRepPack();

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
