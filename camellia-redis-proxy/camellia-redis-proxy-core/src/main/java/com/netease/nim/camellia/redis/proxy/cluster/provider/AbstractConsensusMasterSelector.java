package com.netease.nim.camellia.redis.proxy.cluster.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by caojiajun on 2024/6/28
 */
public abstract class AbstractConsensusMasterSelector implements ConsensusMasterSelector {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConsensusMasterSelector.class);

    private final CopyOnWriteArrayList<ConsensusMasterChangeListener> masterChangeListenerList = new CopyOnWriteArrayList<>();


    @Override
    public void addConsensusMasterChangeListener(ConsensusMasterChangeListener listener) {
        masterChangeListenerList.add(listener);
    }

    protected final void notifyMasterChange() {
        for (ConsensusMasterChangeListener listener : masterChangeListenerList) {
            try {
                listener.change();
            } catch (Exception e) {
                logger.error("master change listener error", e);
            }
        }
    }
}
