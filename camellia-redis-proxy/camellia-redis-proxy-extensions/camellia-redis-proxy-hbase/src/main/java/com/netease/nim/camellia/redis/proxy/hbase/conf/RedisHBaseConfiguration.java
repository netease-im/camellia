package com.netease.nim.camellia.redis.proxy.hbase.conf;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.utils.SysUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/2/27.
 */
public class RedisHBaseConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseConfiguration.class);

    private static volatile Map<String, String> redisKeyTypePrioriConfMap = null;
    static {
        ProxyDynamicConf.registerCallback(() -> redisKeyTypePrioriConfMap = null);
    }

    //hbase表名
    public static String hbaseTableName() {
        return ProxyDynamicConf.getString("hbase.table.name", "nim:nim_camellia");
    }

    //是否开启监控，RedisHBaseMonitor
    public static boolean monitorEnable() {
        return ProxyDynamicConf.getBoolean("camellia.redis.proxy.hbase.monitor.enable", true);
    }

    //redis key前缀
    public static String redisKeyPrefix() {
        return ProxyDynamicConf.getString("redis.key.prefix", "c_");
    }

    //zset的member超过多少字节开启冷热
    public static int zsetMemberRefKeyThreshold() {
        return ProxyDynamicConf.getInt("zset.member.ref.key.threshold", 48);
    }

    //string的value超过多少字节开启冷热
    public static int stringValueThreshold() {
        return ProxyDynamicConf.getInt("string.value.threshold", 0);
    }

    //hash的value超过多少字节开启冷热
    public static int hashMemberRefKeyThreshold() {
        return ProxyDynamicConf.getInt("hash.member.ref.key.threshold", 48);
    }

    //zset的member引用缓存时间，单位秒
    public static int zsetMemberRefKeyExpireSeconds() {
        return ProxyDynamicConf.getInt("zset.member.ref.key.cache.expire.seconds", 24 * 3600);
    }

    //string的value引用缓存时间，单位秒
    public static int stringValueExpireSeconds() {
        return ProxyDynamicConf.getInt("string.value.cache.expire.seconds", 2 * 3600);
    }

    //hash的value引用缓存时间，单位秒
    public static int hashMemberRefKeyExpireSeconds() {
        return ProxyDynamicConf.getInt("hash.member.ref.key.cache.expire.seconds", 2 * 3600);
    }

    //redis pipeline操作的最大值
    public static int redisMaxPipeline() {
        return ProxyDynamicConf.getInt("redis.max.pipeline", 100);
    }

    //hbase的batch操作的最大值
    public static int hbaseMaxBatch() {
        return ProxyDynamicConf.getInt("hbase.max.batch", 100);
    }

    //hbase是否开启异步写
    public static boolean hbaseAsyncWriteEnable() {
        return ProxyDynamicConf.getBoolean("hbase.async.write.enable", true);
    }

    //hbase异步写线程数量
    public static int hbaseAsyncWritePoolSize() {
        return ProxyDynamicConf.getInt("hbase.async.write.pool.size", SysUtils.getCpuNum() * 2);
    }

    //hbase异步写队列大小
    public static int hbaseAsyncWriteQueueSize() {
        return ProxyDynamicConf.getInt("hbase.async.write.queue.size", 1000000);
    }

    //hbase异步写任务提交失败时是否降级，若降级则丢弃，否则改成同步写
    public static boolean hbaseDegradedIfAsyncWriteSubmitFail() {
        return ProxyDynamicConf.getBoolean("hbase.degraded.if.async.write.submit.fail", false);
    }

    //hbase读操作是否降级
    public static boolean hbaseReadDegraded() {
        return ProxyDynamicConf.getBoolean("hbase.read.degraded", false);
    }

    //hbase读操作是否开启单机频控
    public static boolean hbaseReadFreqEnable() {
        return ProxyDynamicConf.getBoolean("hbase.read.freq.enable", false);
    }

    //hbase读操作单机频控的检查周期
    public static long hbaseReadFreqCheckMillis() {
        return ProxyDynamicConf.getLong("hbase.read.freq.check.millis", 1000L);
    }

    //hbase读操作单机频控的检查阈值
    public static long hbaseReadFreqCheckThreshold() {
        return ProxyDynamicConf.getLong("hbase.read.freq.check.threshold", 500L);
    }

    //哪些前缀的key是什么类型的一些先验知识，可以用于减少redis请求量
    public static Map<String, String> redisKeyTypePrioriConf() {
        if (redisKeyTypePrioriConfMap != null) return redisKeyTypePrioriConfMap;
        String string = ProxyDynamicConf.getString("redis.key.type.priori.conf", null);
        Map<String, String> map = new HashMap<>();
        try {
            if (string != null) {
                JSONObject json = JSONObject.parseObject(string);
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    String keyPrefix = entry.getKey();
                    String type = String.valueOf(entry.getValue());
                    if (type.equals("string") || type.equals("hash") ||
                            type.equals("zset") || type.equals("list") || type.equals("set")) {
                        map.put(keyPrefix, type);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("parse redis.key.type.priori.conf error", e);
        }
        redisKeyTypePrioriConfMap = map;
        return redisKeyTypePrioriConfMap;
    }
}
