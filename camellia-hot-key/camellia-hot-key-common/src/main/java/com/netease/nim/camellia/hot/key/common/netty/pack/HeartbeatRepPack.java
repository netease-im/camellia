package com.netease.nim.camellia.hot.key.common.netty.pack;


import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HeartbeatRepPack extends HotKeyPackBody {

    public static final HeartbeatRepPack INSTANCE = new HeartbeatRepPack();

    @Override
    public void marshal(Pack pack) {
    }

    @Override
    public void unmarshal(Unpack unpack) {
    }
}
