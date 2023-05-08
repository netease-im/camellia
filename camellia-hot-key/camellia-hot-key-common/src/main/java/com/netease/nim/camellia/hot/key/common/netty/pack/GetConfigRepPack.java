package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * 获取配置的响应包
 * Created by caojiajun on 2023/5/8
 */
public class GetConfigRepPack extends HotKeyPackBody {

    private HotKeyConfig config;

    public GetConfigRepPack() {
    }

    public GetConfigRepPack(HotKeyConfig config) {
        this.config = config;
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
