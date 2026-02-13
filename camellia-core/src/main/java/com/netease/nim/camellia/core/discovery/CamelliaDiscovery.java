package com.netease.nim.camellia.core.discovery;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/2
 */
public interface CamelliaDiscovery {

    List<ServerNode> findAll();

    void setCallback(Callback callback);

    void clearCallback(Callback callback);

    interface Callback {

        void add(ServerNode server);

        void remove(ServerNode server);
    }
}
