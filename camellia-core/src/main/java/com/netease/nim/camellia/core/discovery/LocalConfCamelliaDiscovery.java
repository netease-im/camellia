package com.netease.nim.camellia.core.discovery;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/2
 */
public class LocalConfCamelliaDiscovery implements CamelliaDiscovery {
    private final List<ServerNode> list;

    public LocalConfCamelliaDiscovery(List<ServerNode> list) {
        this.list = list;
    }

    @Override
    public List<ServerNode> findAll() {
        return list;
    }

    @Override
    public void setCallback(Callback callback) {
    }

    @Override
    public void clearCallback(Callback callback) {
    }
}
