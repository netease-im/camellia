package com.netease.nim.camellia.http.accelerate.proxy.core.conf;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DynamicConf {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConf.class);

    private static Map<String, String> conf = new HashMap<>();
    private static final AtomicBoolean hasInit = new AtomicBoolean(false);

    public static void init() {
        if (hasInit.compareAndSet(false, true)) {
            reload();
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("dynamic-conf"))
                    .scheduleAtFixedRate(DynamicConf::reload, 60, 60, TimeUnit.SECONDS);
        }
    }

    public static void reload() {
        Properties props = ConfigurationUtil.load("proxy.properties");
        Map<String, String> map = new HashMap<>();
        if (props != null) {
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                map.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        if (map.equals(conf)) {
            if (logger.isDebugEnabled()) {
                logger.debug("DynamicConf skip reload for conf not modify");
            }
        } else {
            conf = map;
            logger.info("DynamicConf reload success.");
        }
    }

    public static int getInt(String key, int defaultValue) {
        return ConfigurationUtil.getInteger(conf, key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return ConfigurationUtil.getLong(conf, key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return ConfigurationUtil.get(conf, key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return ConfigurationUtil.getBoolean(conf, key, defaultValue);
    }
}
