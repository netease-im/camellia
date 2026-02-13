package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/3/7
 */
public class ReloadableCamelliaDiscovery extends AbstractCamelliaDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableCamelliaDiscovery.class);

    private static final ScheduledExecutorService defaultSchedule = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new CamelliaThreadFactory(ReloadableCamelliaDiscovery.class));

    private final AtomicBoolean initOk = new AtomicBoolean(false);

    private ServerNodeGetter serverListGetter;
    private Set<ServerNode> set;

    public ReloadableCamelliaDiscovery() {
    }

    public ReloadableCamelliaDiscovery(ServerNodeGetter serverListGetter, long reloadIntervalSeconds) {
        init(serverListGetter, reloadIntervalSeconds, defaultSchedule);
    }

    public ReloadableCamelliaDiscovery(ServerNodeGetter serverListGetter, long reloadIntervalSeconds, ScheduledExecutorService schedule) {
        init(serverListGetter, reloadIntervalSeconds, schedule);
    }

    protected void init(ServerNodeGetter serverListGetter, long reloadIntervalSeconds) {
        init(serverListGetter, reloadIntervalSeconds, null);
    }

    protected void init(ServerNodeGetter serverListGetter, long reloadIntervalSeconds, ScheduledExecutorService schedule) {
        if (initOk.compareAndSet(false, true)) {
            this.serverListGetter = serverListGetter;
            this.set = new HashSet<>(serverListGetter.findAll());
            if (schedule == null) {
                schedule = defaultSchedule;
            }
            schedule.scheduleAtFixedRate(new RefreshThread(this), reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
        } else {
            throw new IllegalStateException("duplicate init");
        }
    }

    public static interface ServerNodeGetter {
        List<ServerNode> findAll();
    }

    @Override
    public List<ServerNode> findAll() {
        if (!initOk.get()) {
            throw new IllegalStateException("not init");
        }
        return serverListGetter.findAll();
    }

    private static class RefreshThread implements Runnable {

        private final ReloadableCamelliaDiscovery discovery;

        RefreshThread(ReloadableCamelliaDiscovery discovery) {
            this.discovery = discovery;
        }

        @Override
        public void run() {
            try {
                Set<ServerNode> newSet = new HashSet<>(discovery.serverListGetter.findAll());
                Set<ServerNode> oldSet = new HashSet<>(discovery.set);

                //new - old = add
                Set<ServerNode> addSet = new HashSet<>(newSet);
                addSet.removeAll(oldSet);
                if (!addSet.isEmpty()) {
                    List<ServerNode> added = new ArrayList<>(addSet);
                    Collections.shuffle(added);
                    //callback add
                    for (ServerNode server : added) {
                        try {
                            discovery.invokeAddCallback(server);
                        } catch (Exception e) {
                            logger.error("callback add error", e);
                        }
                    }
                }

                //old - new = remove
                Set<ServerNode> removeSet = new HashSet<>(oldSet);
                removeSet.removeAll(newSet);
                if (!removeSet.isEmpty()) {
                    List<ServerNode> removed = new ArrayList<>(removeSet);
                    Collections.shuffle(removed);
                    //callback remove
                    for (ServerNode server : removed) {
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
