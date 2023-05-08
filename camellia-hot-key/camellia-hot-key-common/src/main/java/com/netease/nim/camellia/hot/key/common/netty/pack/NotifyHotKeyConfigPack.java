package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/8
 */
public class NotifyHotKeyConfigPack extends HotKeyPackBody {

    private HotKeyConfig config;

    public NotifyHotKeyConfigPack(HotKeyConfig config) {
        this.config = config;
    }

    public NotifyHotKeyConfigPack() {
    }

    public HotKeyConfig getConfig() {
        return config;
    }

    @Override
    public void marshal(Pack pack) {
        HotKeyConfigPackUtils.marshal(config, pack);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        this.config = HotKeyConfigPackUtils.unmarshal(unpack);
    }
}
