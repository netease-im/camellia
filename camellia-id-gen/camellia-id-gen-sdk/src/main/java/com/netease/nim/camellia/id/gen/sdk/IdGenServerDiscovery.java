package com.netease.nim.camellia.id.gen.sdk;

import java.util.List;

/**
 * Created by caojiajun on 2021/9/29
 */
public interface IdGenServerDiscovery {

    List<IdGenServer> findAll();

    void setCallback(Callback callback);

    void clearCallback(Callback callback);

    interface Callback {

        void add(IdGenServer server);

        void remove(IdGenServer server);
    }
}
