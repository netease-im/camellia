package com.netease.nim.camellia.core.discovery;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/3/8
 */
public abstract class SimpleHealthChecker<T> implements CamelliaServerHealthChecker<T> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHealthChecker.class);

    private final OkHttpClient okHttpClient;
    private final Map<String, String> headers;
    private final int healthCacheSeconds;
    private final ConcurrentLinkedHashMap<String, CacheBean> cache = new ConcurrentLinkedHashMap.Builder<String, CacheBean>()
            .initialCapacity(2000)
            .maximumWeightedCapacity(100000)
            .build();

    public SimpleHealthChecker(OkHttpClient okHttpClient, Map<String, String> headers, int healthCacheSeconds) {
        this.okHttpClient = okHttpClient;
        this.headers = headers;
        this.healthCacheSeconds = healthCacheSeconds;
        if (healthCacheSeconds > 0) {
            schedule();
        }
    }

    public SimpleHealthChecker(OkHttpClient okHttpClient, int healthCacheSeconds) {
        this(okHttpClient, new HashMap<>(), healthCacheSeconds);
    }

    public SimpleHealthChecker(OkHttpClient okHttpClient) {
        this(okHttpClient, new HashMap<>(), 5);
    }

    public SimpleHealthChecker(int healthCacheSeconds) {
        this(new OkHttpClient(), new HashMap<>(), healthCacheSeconds);
    }

    public SimpleHealthChecker() {
        this(new OkHttpClient(), new HashMap<>(), 5);
    }

    public abstract String toUrl(T server);

    @Override
    public boolean healthCheck(T server) {
        String url = toUrl(server);
        if (healthCacheSeconds <= 0) {
            return check(url);
        }
        CacheBean cacheBean = cache.get(url);
        if (cacheBean == null) {
            //第一次请求
            cacheBean = new CacheBean();
            cacheBean.pass = check(url);
            long now = System.currentTimeMillis();
            cacheBean.lastCheckTime = now;
            cacheBean.lastHitTime = now;
            cache.put(url, cacheBean);
            return cacheBean.pass;
        }
        //如果超过指定时间+1s，还没有检查过，则检查一下吧
        if (System.currentTimeMillis() - cacheBean.lastCheckTime > (healthCacheSeconds + 1)*1000L) {
            //只穿透一个请求
            if (cacheBean.lock.compareAndSet(false, true)) {
                try {
                    cacheBean.pass = check(url);
                    cacheBean.lastCheckTime = System.currentTimeMillis();
                } finally {
                    cacheBean.lock.compareAndSet(true, false);
                }
            }
        }
        cacheBean.lastHitTime = System.currentTimeMillis();
        return cacheBean.pass;
    }

    private static class CacheBean {
        boolean pass;
        long lastCheckTime;
        long lastHitTime;
        AtomicBoolean lock = new AtomicBoolean();
    }

    private void schedule() {
        if (healthCacheSeconds > 0) {
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(SimpleHealthChecker.class)).scheduleAtFixedRate(() -> {
                try {
                    Set<String> expireUrlSet = new HashSet<>();
                    for (Map.Entry<String, CacheBean> entry : cache.entrySet()) {
                        String url = entry.getKey();
                        CacheBean cacheBean = entry.getValue();
                        //指定时间内有请求，并且指定时间内没有探测过了，则探测一次
                        if ((System.currentTimeMillis() - cacheBean.lastHitTime < healthCacheSeconds*2*1000L)
                                && (System.currentTimeMillis() - cacheBean.lastCheckTime >= healthCacheSeconds*1000L)) {
                            //只穿透一个请求
                            if (cacheBean.lock.compareAndSet(false, true)) {
                                try {
                                    cacheBean.pass = check(url);
                                    cacheBean.lastCheckTime = System.currentTimeMillis();
                                } finally {
                                    cacheBean.lock.compareAndSet(true, false);
                                }
                            }
                        }
                        //很久没有使用过了，则移除吧
                        if (System.currentTimeMillis() - cacheBean.lastHitTime > healthCacheSeconds*120*1000L) {
                            expireUrlSet.add(url);
                        }
                    }
                    for (String url : expireUrlSet) {
                        cache.remove(url);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }, healthCacheSeconds, healthCacheSeconds, TimeUnit.SECONDS);
        }
    }

    private boolean check(String url) {
        Response response = null;
        try {
            Request.Builder builder = new Request.Builder().url(url);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = builder.build();
            response = okHttpClient.newCall(request).execute();
            String string = response.body().string();
            if (logger.isTraceEnabled()) {
                logger.trace("ping response = {}", string);
            }
            boolean alive = response.isSuccessful();
            if (!alive) {
                logger.warn("{} not alive! code = {}", url, response.code());
            }
            return alive;
        } catch (Exception e) {
            logger.warn("{} not alive! ex = {}", url, e.toString());
            return false;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
