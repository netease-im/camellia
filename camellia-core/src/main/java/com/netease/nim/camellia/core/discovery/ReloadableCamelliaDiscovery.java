package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/3/7
 */
public class ReloadableCamelliaDiscovery<T> extends AbstractCamelliaDiscovery<T> {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableCamelliaDiscovery.class);

    private final AtomicBoolean initOk = new AtomicBoolean(false);

    private ServerListGetter<T> serverListGetter;
    private Set<T> set;

    public ReloadableCamelliaDiscovery() {
    }

    public ReloadableCamelliaDiscovery(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds) {
        init(serverListGetter, reloadIntervalSeconds);
    }

    protected void init(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds) {
        if (initOk.compareAndSet(false, true)) {
            this.serverListGetter = serverListGetter;
            this.set = new HashSet<>(serverListGetter.findAll());
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ReloadableCamelliaDiscovery.class))
                    .scheduleAtFixedRate(new RefreshThread<>(this), reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
        } else {
            throw new IllegalStateException("duplicate init");
        }
    }

    public static interface ServerListGetter<T> {
        List<T> findAll();
    }

    @Override
    public List<T> findAll() {
        if (!initOk.get()) {
            throw new IllegalStateException("not init");
        }
        return serverListGetter.findAll();
    }

    private static class RefreshThread<T> implements Runnable {

        private final ReloadableCamelliaDiscovery<T> discovery;

        RefreshThread(ReloadableCamelliaDiscovery<T> discovery) {
            this.discovery = discovery;
        }

        @Override
        public void run() {
            try {
                Set<T> newSet = new HashSet<>(discovery.serverListGetter.findAll());
                Set<T> oldSet = new HashSet<>(discovery.set);

                //new - old = add
                Set<T> addSet = new HashSet<>(newSet);
                addSet.removeAll(oldSet);
                if (!addSet.isEmpty()) {
                    //callback add
                    for (T server : addSet) {
                        try {
                            discovery.invokeAddCallback(server);
                        } catch (Exception e) {
                            logger.error("callback add error", e);
                        }
                    }
                }

                //old - new = remove
                Set<T> removeSet = new HashSet<>(oldSet);
                removeSet.removeAll(newSet);
                if (!removeSet.isEmpty()) {
                    //callback remove
                    for (T server : removeSet) {
                        try {
                            discovery.invokeRemoveCallback(server);
                        } catch (Exception e) {
                            logger.error("callback remove error", e);
                        }
                    }
                }

                discovery.set = newSet;
            } catch (Exception e) {
                logger.error("RefreshThread error", e);
            }
        }
    }
}
