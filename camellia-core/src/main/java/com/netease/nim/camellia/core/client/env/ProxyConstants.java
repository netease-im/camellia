package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.tools.utils.SysUtils;


/**
 *
 * Created by caojiajun on 2020/2/5.
 */
public class ProxyConstants {

    public static final boolean shardingConcurrentEnable = true;
    public static final int shardingConcurrentExecPoolSize = SysUtils.getCpuNum() * 32;

    public static final int multiWriteConcurrentExecPoolSize = SysUtils.getCpuNum() * 32;
    public static final int multiWriteAsyncExecPoolSize = SysUtils.getCpuNum() * 32;
    public static final int multiWriteAsyncExecQueueSize = 100000;
}
