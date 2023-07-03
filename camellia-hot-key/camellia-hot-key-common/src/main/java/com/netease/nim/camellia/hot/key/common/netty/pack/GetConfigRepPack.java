package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;

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
        if (config != null) {
            pack.putBoolean(true);
            HotKeyConfigPackUtils.marshal(config, pack);
        } else {
            pack.putBoolean(false);
        }
    }

    @Override
    public void unmarshal(Unpack unpack) {
        boolean exits = unpack.popBoolean();
        if (exits) {
            this.config = HotKeyConfigPackUtils.unmarshal(unpack);
        }
    }
}
