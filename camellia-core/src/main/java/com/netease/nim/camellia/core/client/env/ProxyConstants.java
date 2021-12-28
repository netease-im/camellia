package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.util.SysUtils;


/**
 *
 * Created by caojiajun on 2020/2/5.
 */
public class ProxyConstants {

    public static final boolean shardingConcurrentEnable = true;
    public static final int shardingConcurrentExecPoolSize = SysUtils.getCpuNum() * 16;
    public static final boolean multiWriteConcurrentEnable = true;
    public static final int multiWriteConcurrentExecPoolSize = SysUtils.getCpuNum() * 16;
}
