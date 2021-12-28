package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.connectlimit.ConnectLimiter;
import com.netease.nim.camellia.redis.proxy.command.async.converter.*;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheStatsCallback;
import com.netease.nim.camellia.redis.proxy.command.async.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.SlowCommandMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfHook;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2021/9/29
 */
@Component
public class CamelliaRedisProxyConfigurerSupport {

    /**
     * 监控数据回调
     */
    @Autowired(required = false)
    private MonitorCallback monitorCallback;

    /**
     * 命令拦截器接
     */
    @Autowired(required = false)
    private CommandInterceptor commandInterceptor;

    /**
     * 慢查询回调
     */
    @Autowired(required = false)
    private SlowCommandMonitorCallback slowCommandMonitorCallback;

    /**
     * 大key监控回调
     */
    @Autowired(required = false)
    private BigKeyMonitorCallback bigKeyMonitorCallback;

    /**
     * 热key监控回调
     */
    @Autowired(required = false)
    private HotKeyMonitorCallback hotKeyMonitorCallback;

    /**
     * 哪些key需要进行热key缓存
     */
    @Autowired(required = false)
    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;

    /**
     * 热key缓存命中统计信息回调
     */
    @Autowired(required = false)
    private HotKeyCacheStatsCallback hotKeyCacheStatsCallback;

    /**
     * 自定义auth策略
     */
    @Autowired(required = false)
    private ClientAuthProvider clientAuthProvider;

    /**
     * key自定义转换器
     */
    @Autowired(required = false)
    private KeyConverter keyConverter;

    /**
     * string的自定义value转换器
     */
    @Autowired(required = false)
    private StringConverter stringConverter;

    /**
     * list的自定义value转换器
     */
    @Autowired(required = false)
    private ListConverter listConverter;

    /**
     * set的自定义value转换器
     */
    @Autowired(required = false)
    private SetConverter setConverter;

    /**
     * zset的自定义value转换器
     */
    @Autowired(required = false)
    private ZSetConverter zSetConverter;

    /**
     * hash的自定义value转换器
     */
    @Autowired(required = false)
    private HashConverter hashConverter;

    /**
     * 自定义分片函数
     */
    @Autowired(required = false)
    private ShardingFunc shardingFunc;

    /**
     * 动态配置hook
     */
    @Autowired(required = false)
    private ProxyDynamicConfHook proxyDynamicConfHook;

    /**
     * 自定义动态路由
     */
    @Autowired(required = false)
    private ProxyRouteConfUpdater proxyRouteConfUpdater;

    /**
     * 客户端连接限制
     */
    @Autowired(required = false)
    private ConnectLimiter connectLimiter;

    /**
     * 动态配置回调
     */
    @Autowired(required = false)
    private ProxyDynamicConfSupport proxyDynamicConfSupport;

    public MonitorCallback getMonitorCallback() {
        return monitorCallback;
    }

    public CommandInterceptor getCommandInterceptor() {
        return commandInterceptor;
    }

    public SlowCommandMonitorCallback getSlowCommandMonitorCallback() {
        return slowCommandMonitorCallback;
    }

    public BigKeyMonitorCallback getBigKeyMonitorCallback() {
        return bigKeyMonitorCallback;
    }

    public HotKeyMonitorCallback getHotKeyMonitorCallback() {
        return hotKeyMonitorCallback;
    }

    public HotKeyCacheStatsCallback getHotKeyCacheStatsCallback() {
        return hotKeyCacheStatsCallback;
    }

    public ClientAuthProvider getClientAuthProvider() {
        return clientAuthProvider;
    }

    public KeyConverter getKeyConverter() {
        return keyConverter;
    }

    public StringConverter getStringConverter() {
        return stringConverter;
    }

    public ListConverter getListConverter() {
        return listConverter;
    }

    public SetConverter getSetConverter() {
        return setConverter;
    }

    public ZSetConverter getzSetConverter() {
        return zSetConverter;
    }

    public HashConverter getHashConverter() {
        return hashConverter;
    }

    public ShardingFunc getShardingFunc() {
        return shardingFunc;
    }

    public HotKeyCacheKeyChecker getHotKeyCacheKeyChecker() {
        return hotKeyCacheKeyChecker;
    }

    public ProxyDynamicConfHook getProxyDynamicConfHook() {
        return proxyDynamicConfHook;
    }

    public ProxyRouteConfUpdater getProxyRouteConfUpdater() {
        return proxyRouteConfUpdater;
    }

    public ConnectLimiter getConnectLimiter() {
        return connectLimiter;
    }

    public ProxyDynamicConfSupport getProxyDynamicConfSupport() {
        return proxyDynamicConfSupport;
    }
}
