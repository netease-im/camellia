package com.netease.nim.camellia.core.api;


import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2019/6/5.
 */
public class RemoteMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(RemoteMonitor.class);

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new CamelliaThreadFactory("camellia-monitor"));

    private final ConcurrentHashMap<String, LongAdder> readMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> writeMap = new ConcurrentHashMap<>();

    private final Long bid;
    private final String bgroup;
    private final CamelliaApi service;

    public RemoteMonitor(Long bid, String bgroup, CamelliaApi service) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.service = service;
        scheduleService.scheduleAtFixedRate(() -> {
            try {
                calcAndReport();
            } catch (Exception e) {
                logger.error("calc error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void incrWrite(String resource, String className, String methodName) {
        try {
            String key = resource + "|" + className + "|" + methodName + "|" + ResourceStats.OPE_WRITE;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(writeMap, key, k -> new LongAdder());
            count.increment();
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
            String key = resource + "|" + className + "|" + methodName + "|" + ResourceStats.OPE_READ;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(readMap, key, k -> new LongAdder());
            count.increment();
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
        for (Map.Entry<String, LongAdder> entry : readMap.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            String resource = split[0];
            String className = split[1];
            String methodName = split[2];
            String ope = split[3];
            DetailUniqueKey key = new DetailUniqueKey(resource, className, methodName, ope);

            long read = entry.getValue().sumThenReset();
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

        for (Map.Entry<String, LongAdder> entry : writeMap.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            String resource = split[0];
            String className = split[1];
            String methodName = split[2];
            String ope = split[3];
            DetailUniqueKey key = new DetailUniqueKey(resource, className, methodName, ope);

            long write = entry.getValue().sumThenReset();
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

            if (!Objects.equals(resource, uniqueKey.resource)) return false;
            return Objects.equals(ope, uniqueKey.ope);
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

            if (!Objects.equals(resource, that.resource)) return false;
            if (!Objects.equals(className, that.className)) return false;
            if (!Objects.equals(methodName, that.methodName)) return false;
            return Objects.equals(ope, that.ope);
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
