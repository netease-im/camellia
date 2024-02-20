package com.netease.nim.camellia.mq.isolation.core.env;

import com.netease.nim.camellia.tools.utils.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationEnv {

    private static final Logger logger = LoggerFactory.getLogger(MqIsolationEnv.class);

    public static int monitorIntervalSeconds = 60;
    public final static String instanceId = UUID.randomUUID().toString().replace("-", "");
    public static String host;
    static {
        try {
            InetAddress address = InetUtils.findFirstNonLoopbackAddress();
            if (address != null) {
                host = address.getHostAddress();
            } else {
                host = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            host = "unknown";
        }
    }
}
