package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.console.ConsoleResult;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
@Component
public class MyConsoleService extends ConsoleServiceAdaptor {

    @Override
    public ConsoleResult online() {
        //TODO register to 注册中心，或者挂载到负载均衡服务器
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        //TODO deregister from 注册中心，或者从负载均衡服务器上摘掉
        return super.offline();
    }

    @Override
    public ConsoleResult custom(Map<String, List<String>> params) {
        if (params.containsKey("redisHBaseNss")) {
            return ConsoleResult.success(RedisHBaseMonitor.getStatsJson().toJSONString());
        }
        return super.custom(params);
    }

}
