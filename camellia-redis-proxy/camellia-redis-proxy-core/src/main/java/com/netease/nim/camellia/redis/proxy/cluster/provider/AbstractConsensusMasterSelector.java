package com.netease.nim.camellia.redis.proxy.cluster.provider;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by caojiajun on 2024/6/28
 */
public abstract class AbstractConsensusMasterSelector implements ConsensusMasterSelector {

    private final CopyOnWriteArrayList<ConsensusMasterChangeListener> masterChangeListenerList = new CopyOnWriteArrayList<>();


    @Override
    public void addConsensusMasterChangeListener(ConsensusMasterChangeListener listener) {
        masterChangeListenerList.add(listener);
    }

    protected final void notifyMasterChange() {
        for (ConsensusMasterChangeListener listener : masterChangeListenerList) {
            listener.change();
        }
    }
}
