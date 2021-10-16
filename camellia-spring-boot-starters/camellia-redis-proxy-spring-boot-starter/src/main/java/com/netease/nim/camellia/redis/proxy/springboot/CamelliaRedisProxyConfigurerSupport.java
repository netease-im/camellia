package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.client.env.ShadingFunc;
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

    @Autowired(required = false)
    private MonitorCallback monitorCallback;

    @Autowired(required = false)
    private CommandInterceptor commandInterceptor;

    @Autowired(required = false)
    private SlowCommandMonitorCallback slowCommandMonitorCallback;

    @Autowired(required = false)
    private BigKeyMonitorCallback bigKeyMonitorCallback;

    @Autowired(required = false)
    private HotKeyMonitorCallback hotKeyMonitorCallback;

    @Autowired(required = false)
    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;

    @Autowired(required = false)
    private HotKeyCacheStatsCallback hotKeyCacheStatsCallback;

    @Autowired(required = false)
    private ClientAuthProvider clientAuthProvider;

    @Autowired(required = false)
    private KeyConverter keyConverter;

    @Autowired(required = false)
    private StringConverter stringConverter;

    @Autowired(required = false)
    private ListConverter listConverter;

    @Autowired(required = false)
    private SetConverter setConverter;

    @Autowired(required = false)
    private ZSetConverter zSetConverter;

    @Autowired(required = false)
    private HashConverter hashConverter;

    @Autowired(required = false)
    private ShadingFunc shadingFunc;

    @Autowired(required = false)
    private ProxyDynamicConfHook proxyDynamicConfHook;

    @Autowired(required = false)
    private ProxyRouteConfUpdater proxyRouteConfUpdater;

    @Autowired(required = false)
    private ConnectLimiter connectLimiter;

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

    public ShadingFunc getShadingFunc() {
        return shadingFunc;
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
}
