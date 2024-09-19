package com.netease.nim.camellia.id.gen.springboot.segment;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.sdk.CamelliaIdGenHttpUtils;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class IdSyncInMultiRegionService {

    private static final Logger logger = LoggerFactory.getLogger(IdSyncInMultiRegionService.class);

    private final CamelliaIdGenSegmentProperties.IdSyncInMultiRegionsConf syncConf;
    private final IDLoader idLoader;
    private OkHttpClient okHttpClient;

    public IdSyncInMultiRegionService(IDLoader idLoader, CamelliaIdGenSegmentProperties.IdSyncInMultiRegionsConf syncConf) {
        this.syncConf = syncConf;
        this.idLoader = idLoader;
        if (syncConf != null && syncConf.isEnable() && idLoader != null) {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(2048);
            dispatcher.setMaxRequestsPerHost(1024);
            okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(syncConf.getApiTimeoutMillis(), TimeUnit.MILLISECONDS)
                    .writeTimeout(syncConf.getApiTimeoutMillis(), TimeUnit.MILLISECONDS)
                    .connectTimeout(syncConf.getApiTimeoutMillis(), TimeUnit.MILLISECONDS)
                    .dispatcher(dispatcher)
                    .connectionPool(new ConnectionPool(1024, 5, TimeUnit.MINUTES))
                    .build();
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("id-sync-in-multi-regions"))
                    .scheduleAtFixedRate(this::sync, syncConf.getCheckIntervalSeconds() + ThreadLocalRandom.current().nextInt(60),
                            syncConf.getCheckIntervalSeconds(), TimeUnit.SECONDS);
            logger.info("id sync in multi regions schedule task start, api-timeout-millis = {}, check-interval-seconds = {}",
                    syncConf.getApiTimeoutMillis(), syncConf.getCheckIntervalSeconds());
        }
    }

    public boolean sync() {
        try {
            if (syncConf == null || !syncConf.isEnable()) {
                logger.warn("id sync not enable");
                return false;
            }
            if (idLoader == null) {
                logger.warn("id loader is null");
                return false;
            }
            List<String> whiteListTags = syncConf.getWhiteListTags();
            List<String> urls = syncConf.getMultiRegionUrls();
            if (urls.isEmpty() || urls.size() <= 1) {
                logger.warn("multi region urls.size = {}, skip sync", urls.size());
                return false;
            }
            logger.info("id sync in multi regions start, urls = {}", urls);
            if (whiteListTags != null && !whiteListTags.isEmpty()) {
                logger.info("sync id of white list tags = {}", whiteListTags);
                for (String tag : whiteListTags) {
                    Map<String, Long> tagIdMap = new HashMap<>();
                    Long maxId = -1L;
                    for (String url : urls) {
                        Long id = selectId(url, tag);
                        logger.info("select id, url = {}, tag = {}, id = {}", url, tag, id);
                        if (id == null) id = 0L;
                        tagIdMap.put(url, id);
                        if (id > maxId) {
                            maxId = id;
                        }
                    }
                    if (!tagIdMap.isEmpty() && maxId > 0) {
                        for (Map.Entry<String, Long> entry : tagIdMap.entrySet()) {
                            String url = entry.getKey();
                            Long id = entry.getValue();
                            if (maxId - id > syncConf.getIdUpdateThreshold()) {
                                boolean update = update(url, tag, maxId);
                                logger.info("update id, url = {}, tag = {}, maxId = {}, result = {}", url, tag, maxId, update);
                            }
                        }
                    }
                }
            } else {
                List<String> blackListTags = syncConf.getBlackListTags();
                logger.info("sync id of all tags, black list tags = {}", blackListTags);
                Set<String> tagSet = new HashSet<>();
                Map<String, Map<String, Long>> map = new HashMap<>();
                for (String url : urls) {
                    Map<String, Long> subMap = selectTagIdMaps(url);
                    logger.info("select tag id maps, url = {}, size = {}", url, subMap.size());
                    tagSet.addAll(subMap.keySet());
                    map.put(url, subMap);
                }
                for (String tag : tagSet) {
                    if (blackListTags.contains(tag)) {
                        continue;
                    }
                    Map<String, Long> tagIdMap = new HashMap<>();
                    long maxId = -1L;
                    for (String url : urls) {
                        Long id = map.get(url).get(tag);
                        logger.info("find id, url = {}, tag = {}, id = {}", url, tag, id);
                        if (id == null) {
                            id = 0L;
                        }
                        if (id > maxId) {
                            maxId = id;
                        }
                        tagIdMap.put(url, id);
                    }
                    if (!tagIdMap.isEmpty() && maxId > 0) {
                        for (Map.Entry<String, Long> entry : tagIdMap.entrySet()) {
                            String url = entry.getKey();
                            Long id = entry.getValue();
                            if (maxId - id > syncConf.getIdUpdateThreshold()) {
                                boolean update = update(url, tag, maxId);
                                logger.info("update id, url = {}, tag = {}, maxId = {}, result = {}", url, tag, maxId, update);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("sync id in multi region error", e);
            return false;
        }
    }

    private Map<String, Long> selectTagIdMaps(String url) {
        JSONObject json = CamelliaIdGenHttpUtils.invokeGet(okHttpClient, url + "/camellia/id/gen/segment/selectTagIdMaps");
        JSONObject data = json.getJSONObject("data");
        Map<String, Long> result = new HashMap<>();
        if (data == null) return result;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result.put(entry.getKey(), Long.parseLong(entry.getValue().toString()));
        }
        return result;
    }

    private Long selectId(String url, String tag) {
        try {
            JSONObject json = CamelliaIdGenHttpUtils.invokeGet(okHttpClient, url + "/camellia/id/gen/segment/selectId?tag=" + URLEncoder.encode(tag, "utf-8"));
            return json.getLong("data");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private boolean update(String url, String tag, Long id) {
        try {
            String body = "tag=" + URLEncoder.encode(tag, "utf-8") +
                    "&id=" + id;
            CamelliaIdGenHttpUtils.invokePost(okHttpClient, url + "/camellia/id/gen/segment/update", body);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }
}
