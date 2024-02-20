package com.netease.nim.camellia.mq.isolation.core.env;

import java.util.UUID;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationEnv {

    public static String instanceId = UUID.randomUUID().toString().replace("-", "");
}
