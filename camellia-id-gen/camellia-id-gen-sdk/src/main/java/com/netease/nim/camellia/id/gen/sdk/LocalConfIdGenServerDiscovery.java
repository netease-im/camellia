package com.netease.nim.camellia.id.gen.sdk;

import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2021/9/29
 */
public class LocalConfIdGenServerDiscovery implements IdGenServerDiscovery {

    private final List<IdGenServer> list;

    public LocalConfIdGenServerDiscovery(List<IdGenServer> list) {
        this.list = list;
    }

    public LocalConfIdGenServerDiscovery(IdGenServer server) {
        this.list = Collections.singletonList(server);
    }

    public LocalConfIdGenServerDiscovery(String url) {
        this.list = Collections.singletonList(new IdGenServer(url));
    }

    @Override
    public List<IdGenServer> findAll() {
        return list;
    }

    @Override
    public void setCallback(Callback<IdGenServer> callback) {

    }

    @Override
    public void clearCallback(Callback<IdGenServer> callback) {

    }
}
