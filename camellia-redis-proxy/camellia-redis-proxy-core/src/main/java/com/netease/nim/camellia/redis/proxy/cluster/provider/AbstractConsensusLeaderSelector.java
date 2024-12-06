package com.netease.nim.camellia.redis.proxy.cluster.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by caojiajun on 2024/6/28
 */
public abstract class AbstractConsensusLeaderSelector implements ConsensusLeaderSelector {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConsensusLeaderSelector.class);

    private final CopyOnWriteArrayList<ConsensusLeaderChangeListener> leaderChangeListenerList = new CopyOnWriteArrayList<>();

    /**
     * default constructor
     */
    public AbstractConsensusLeaderSelector() {
    }

    @Override
    public void addConsensusLeaderChangeListener(ConsensusLeaderChangeListener listener) {
        leaderChangeListenerList.add(listener);
    }

    /**
     * notify leader change
     */
    protected final void notifyLeaderChange() {
        for (ConsensusLeaderChangeListener listener : leaderChangeListenerList) {
            try {
                listener.change();
            } catch (Exception e) {
                logger.error("leader change listener error", e);
            }
        }
    }
}
