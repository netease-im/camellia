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
public class ReloadableCamelliaDiscovery<T> extends AbstractCamelliaDiscovery<T> {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableCamelliaDiscovery.class);

    private static final ScheduledExecutorService defaultSchedule = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new CamelliaThreadFactory(ReloadableCamelliaDiscovery.class));

    private final AtomicBoolean initOk = new AtomicBoolean(false);

    private ServerListGetter<T> serverListGetter;
    private Set<T> set;

    public ReloadableCamelliaDiscovery() {
    }

    public ReloadableCamelliaDiscovery(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds) {
        init(serverListGetter, reloadIntervalSeconds, defaultSchedule);
    }

    public ReloadableCamelliaDiscovery(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds, ScheduledExecutorService schedule) {
        init(serverListGetter, reloadIntervalSeconds, schedule);
    }

    protected void init(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds) {
        init(serverListGetter, reloadIntervalSeconds, null);
    }

    protected void init(ServerListGetter<T> serverListGetter, long reloadIntervalSeconds, ScheduledExecutorService schedule) {
        if (initOk.compareAndSet(false, true)) {
            this.serverListGetter = serverListGetter;
            this.set = new HashSet<>(serverListGetter.findAll());
            if (schedule == null) {
                schedule = defaultSchedule;
            }
            schedule.scheduleAtFixedRate(new RefreshThread<>(this), reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
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
                    List<T> added = new ArrayList<>(addSet);
                    Collections.shuffle(added);
                    //callback add
                    for (T server : added) {
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
                    List<T> removed = new ArrayList<>(removeSet);
                    Collections.shuffle(removed);
                    //callback remove
                    for (T server : removed) {
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
