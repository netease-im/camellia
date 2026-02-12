package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.bigkey.BigKeyProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.converter.ConverterProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyRouteRewriteProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.misc.DelayDoubleDeleteProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.misc.MultiWriteProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.misc.TroubleTrickKeysProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.monitor.MonitorProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.permission.*;

/**
 * 一些内置的plugin，可以通过别名直接开启，而不需要全类目
 * Created by caojiajun on 2022/9/16
 */
public enum ProxyPluginEnums {

    //用于监控，主要是监控请求量和响应时间以及慢查询
    MONITOR_PLUGIN("monitorPlugin", MonitorProxyPlugin.class, Integer.MAX_VALUE, Integer.MIN_VALUE),
    //控制访问权限，ip黑白名单
    IP_CHECKER_PLUGIN("ipCheckerPlugin", IPCheckProxyPlugin.class, Integer.MAX_VALUE - 10000, 0),
    //Dynamic IP Checker, configured by camellia-dashboard
    DYNAMIC_IP_CHECKER_PLUGIN("dynamicIpCheckerPlugin", DynamicIpCheckProxyPlugin.class, Integer.MAX_VALUE - 10000, 0),

    //只读
    READ_ONLY_PLUGIN("readOnlyPlugin", ReadOnlyProxyPlugin.class, Integer.MAX_VALUE - 15000, 0),
    //屏蔽某些命令
    COMMAND_DISABLE_PLUGIN("commandDisablePlugin", CommandDisableProxyPlugin.class, Integer.MAX_VALUE - 20000, 0),
    //用于控制请求速率
    RATE_LIMIT_PLUGIN("rateLimitPlugin", RateLimitProxyPlugin.class, Integer.MAX_VALUE - 30000, 0),
    //Dynamic Rate Limit, configured by camellia-dashboard
    DYNAMIC_RATE_LIMIT_PLUGIN("dynamicRateLimitPlugin", DynamicRateLimitProxyPlugin.class, Integer.MAX_VALUE - 30000, 0),
    //用于拦截非法的key，直接快速失败
    TROUBLE_TRICK_KEYS_PLUGIN("troubleTrickKeysPlugin", TroubleTrickKeysProxyPlugin.class, Integer.MAX_VALUE - 40000, 0),

    //用于监控热key
    HOT_KEY_PLUGIN("hotKeyPlugin", HotKeyProxyPlugin.class, 20000, 0),
    //用于监控热key并转发到自定义路由
    HOT_KEY_ROUTE_REWRITE_PLUGIN("hotKeyRouteRewritePlugin", HotKeyRouteRewriteProxyPlugin.class, 20000, 0),
    //用于热key缓存（仅支持GET命令）
    HOT_KEY_CACHE_PLUGIN("hotKeyCachePlugin", HotKeyCacheProxyPlugin.class, 10000, Integer.MIN_VALUE + 10000),

    //用于监控大key
    BIG_KEY_PLUGIN("bigKeyPlugin", BigKeyProxyPlugin.class, 0, 0),
    //用于缓存双删（仅拦截DEL命令）
    DELAY_DOUBLE_DELETE_PLUGIN("delayDoubleDeletePlugin", DelayDoubleDeleteProxyPlugin.class, 0, 0),
    //用于自定义双写规则（key维度的）
    MULTI_WRITE_PLUGIN("multiWritePlugin", MultiWriteProxyPlugin.class, 0, 0),

    //用于进行key/value的转换
    CONVERTER_PLUGIN("converterPlugin", ConverterProxyPlugin.class, Integer.MIN_VALUE, Integer.MAX_VALUE),

    ;

    private final String alias;
    private final Class<? extends ProxyPlugin> clazz;
    private final int requestOrder;
    private final int replyOrder;

    ProxyPluginEnums(String alias, Class<? extends ProxyPlugin> clazz, int requestOrder, int replyOrder) {
        this.alias = alias;
        this.clazz = clazz;
        this.requestOrder = requestOrder;
        this.replyOrder = replyOrder;
    }

    public String getAlias() {
        return alias;
    }

    public Class<? extends ProxyPlugin> getClazz() {
        return clazz;
    }

    public int getRequestOrder() {
        return ProxyDynamicConf.getInt(alias + ".build.in.plugin.request.order", requestOrder);
    }

    public int getReplyOrder() {
        return ProxyDynamicConf.getInt(alias + ".build.in.plugin.reply.order", replyOrder);
    }

    public static ProxyPluginEnums getByAlias(String alias) {
        for (ProxyPluginEnums pluginEnum : ProxyPluginEnums.values()) {
            if (pluginEnum.alias.equals(alias)) {
                return pluginEnum;
            }
        }
        return null;
    }
}
