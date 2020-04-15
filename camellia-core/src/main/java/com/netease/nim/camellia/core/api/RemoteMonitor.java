package com.netease.nim.camellia.core.api;


import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/6/5.
 */
public class RemoteMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(RemoteMonitor.class);

    private ConcurrentHashMap<DetailUniqueKey, AtomicLong> readMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<DetailUniqueKey, AtomicLong> writeMap = new ConcurrentHashMap<>();

    private Long bid;
    private String bgroup;
    private CamelliaApi service;

    public RemoteMonitor(Long bid, String bgroup, CamelliaApi service) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.service = service;
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RemoteMonitor.class)).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    calcAndReport();
                } catch (Exception e) {
                    logger.error("calc error", e);
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void incrWrite(String resource, String className, String methodName) {
        try {
            DetailUniqueKey uniqueKey = new DetailUniqueKey(resource, className, methodName, ResourceStats.OPE_WRITE);
            AtomicLong count = writeMap.get(uniqueKey);
            if (count == null) {
                count = new AtomicLong(0);
                AtomicLong oldCount = writeMap.putIfAbsent(uniqueKey, count);
                if (oldCount != null) {
                    count = oldCount;
                }
            }
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error("incrWrite error", e);
        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("incrWrite, bid = {}, bgroup = {}, resource = {}, className = {}, methodName = {}",
                        bid, bgroup, resource, className, methodName);
            }
        }
    }

    @Override
    public void incrRead(String resource, String className, String methodName) {
        try {
            DetailUniqueKey uniqueKey = new DetailUniqueKey(resource, className, methodName, ResourceStats.OPE_READ);
            AtomicLong count = readMap.get(uniqueKey);
            if (count == null) {
                count = new AtomicLong(0);
                AtomicLong oldCount = readMap.putIfAbsent(uniqueKey, count);
                if (oldCount != null) {
                    count = oldCount;
                }
            }
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error("incrRead error", e);
        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("incrRead, bid = {}, bgroup = {}, resource = {}, className = {}, methodName = {}",
                        bid, bgroup, resource, className, methodName);
            }
        }
    }

    private void calcAndReport() {
        ResourceStats resourceStats = new ResourceStats();
        Map<UniqueKey, ResourceStats.Stats> map = new HashMap<>();
        Map<DetailUniqueKey, ResourceStats.StatsDetail> detailMap = new HashMap<>();
        for (Map.Entry<DetailUniqueKey, AtomicLong> entry : readMap.entrySet()) {
            DetailUniqueKey key = entry.getKey();
            long read = entry.getValue().getAndSet(0);
            UniqueKey uniqueKey = new UniqueKey(key.getResource(), key.getOpe());
            ResourceStats.Stats stats = map.get(uniqueKey);
            if (stats == null) {
                stats = new ResourceStats.Stats(key.getResource(), key.getOpe());
                map.put(uniqueKey, stats);
            }
            stats.setCount(stats.getCount() + read);

            ResourceStats.StatsDetail detail = detailMap.get(key);
            if (detail == null) {
                detail = new ResourceStats.StatsDetail(key.getResource(), key.getClassName(), key.getMethodName(), ResourceStats.OPE_READ);
                detailMap.put(key, detail);
            }
            detail.setCount(read);
        }

        for (Map.Entry<DetailUniqueKey, AtomicLong> entry : writeMap.entrySet()) {
            DetailUniqueKey key = entry.getKey();
            long write = entry.getValue().getAndSet(0);
            UniqueKey uniqueKey = new UniqueKey(key.getResource(), key.getOpe());
            ResourceStats.Stats stats = map.get(uniqueKey);
            if (stats == null) {
                stats = new ResourceStats.Stats(key.getResource(), key.getOpe());
                map.put(uniqueKey, stats);
            }
            stats.setCount(stats.getCount() + write);

            ResourceStats.StatsDetail detail = detailMap.get(key);
            if (detail == null) {
                detail = new ResourceStats.StatsDetail(key.getResource(), key.getClassName(), key.getMethodName(), ResourceStats.OPE_WRITE);
                detailMap.put(key, detail);
            }
            detail.setCount(write);
        }
        List<ResourceStats.Stats> statsList = new ArrayList<>();
        List<ResourceStats.StatsDetail> statsDetailList = new ArrayList<>();
        for (ResourceStats.Stats stats : map.values()) {
            if (stats.getCount() != 0) {
                statsList.add(stats);
            }
        }
        for (ResourceStats.StatsDetail detail : detailMap.values()) {
            if (detail.getCount() != 0) {
                statsDetailList.add(detail);
            }
        }
        resourceStats.setStatsList(statsList);
        resourceStats.setStatsDetailList(statsDetailList);
        resourceStats.setBid(bid);
        resourceStats.setBgroup(bgroup);
        resourceStats.setSource(CamelliaApiEnv.source);
        if (statsList.isEmpty() && statsDetailList.isEmpty()) {
            return;
        }
        service.reportStats(resourceStats);
    }

    private static class UniqueKey {
        private String resource;
        private String ope;

        public UniqueKey(String resource, String ope) {
            this.resource = resource;
            this.ope = ope;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            UniqueKey uniqueKey = (UniqueKey) object;

            if (resource != null ? !resource.equals(uniqueKey.resource) : uniqueKey.resource != null) return false;
            return ope != null ? ope.equals(uniqueKey.ope) : uniqueKey.ope == null;
        }

        @Override
        public int hashCode() {
            int result = resource != null ? resource.hashCode() : 0;
            result = 31 * result + (ope != null ? ope.hashCode() : 0);
            return result;
        }
    }

    private static class DetailUniqueKey {
        private String resource;
        private String className;
        private String methodName;
        private String ope;

        public DetailUniqueKey(String resource, String className, String methodName, String ope) {
            this.resource = resource;
            this.className = className;
            this.methodName = methodName;
            this.ope = ope;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            DetailUniqueKey that = (DetailUniqueKey) object;

            if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
            if (className != null ? !className.equals(that.className) : that.className != null) return false;
            if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null) return false;
            return ope != null ? ope.equals(that.ope) : that.ope == null;
        }

        @Override
        public int hashCode() {
            int result = resource != null ? resource.hashCode() : 0;
            result = 31 * result + (className != null ? className.hashCode() : 0);
            result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
            result = 31 * result + (ope != null ? ope.hashCode() : 0);
            return result;
        }
    }
}
