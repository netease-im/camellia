## Plugin System
* Plugins use a unified interface to intercept and control requests and responses
* proxy has many built-in plugins that can be used directly with simple configuration
* You can also implement custom plugins. Be sure not to add code logic that may cause blocking behavior in plugins to avoid blocking proxy worker threads

### Plugin Interface
```java
public interface ProxyPlugin {

    /**
     * Initialization method
     */
    default void init(ProxyBeanFactory factory) {
    }

    /**
     * Execute from large to small, request and response can be defined separately
     * @return Priority
     */
    default ProxyPluginOrder order() {
        return ProxyPluginOrder.DEFAULT;
    }

    /**
     * Request (at this time the command has just arrived at proxy, not yet reached backend redis)
     * @param request Context of the request command
     */
    default ProxyPluginResponse executeRequest(ProxyRequest request) {
        return ProxyPluginResponse.SUCCESS;
    }

    /**
     * Response (at this time the command has been responded from backend redis and is about to be returned to the client)
     * @param reply Response reply context
     */
    default ProxyPluginResponse executeReply(ProxyReply reply) {
        return ProxyPluginResponse.SUCCESS;
    }

}
```

### How to Enable Plugins
```
# Plugins can be added or removed dynamically during runtime
proxy.plugin.list=monitorPlugin,bigKeyPlugin,com.xxx.xxx.CustomProxyPlugin
```

### Built-in Plugins
```java
public enum BuildInProxyPluginEnum {

    // Used for monitoring, mainly monitoring request volume, response time, and slow queries
    MONITOR_PLUGIN("monitorPlugin", MonitorProxyPlugin.class, Integer.MAX_VALUE, Integer.MIN_VALUE),
    // Control access permissions, IP blacklist/whitelist
    IP_CHECKER_PLUGIN("ipCheckerPlugin", IPCheckProxyPlugin.class, Integer.MAX_VALUE - 10000, 0),
    // Dynamic IP Checker, configured by camellia-dashboard
    DYNAMIC_IP_CHECKER_PLUGIN("dynamicIpCheckerPlugin", DynamicIpCheckProxyPlugin.class, Integer.MAX_VALUE - 10000, 0),

    // Read-only
    READ_ONLY_PLUGIN("readOnlyPlugin", ReadOnlyProxyPlugin.class, Integer.MAX_VALUE - 15000, 0),
    // Block certain commands
    COMMAND_DISABLE_PLUGIN("commandDisablePlugin", CommandDisableProxyPlugin.class, Integer.MAX_VALUE - 20000, 0),
    // Used to control request rate
    RATE_LIMIT_PLUGIN("rateLimitPlugin", RateLimitProxyPlugin.class, Integer.MAX_VALUE - 30000, 0),
    // Dynamic Rate Limit, configured by camellia-dashboard
    DYNAMIC_RATE_LIMIT_PLUGIN("dynamicRateLimitPlugin", DynamicRateLimitProxyPlugin.class, Integer.MAX_VALUE - 30000, 0),
    // Used to intercept illegal keys for fast failure
    TROUBLE_TRICK_KEYS_PLUGIN("troubleTrickKeysPlugin", TroubleTrickKeysProxyPlugin.class, Integer.MAX_VALUE - 40000, 0),

    // Used to monitor hot keys
    HOT_KEY_PLUGIN("hotKeyPlugin", HotKeyProxyPlugin.class, 20000, 0),
    // Used to monitor hot keys and forward to custom routes
    HOT_KEY_ROUTE_REWRITE_PLUGIN("hotKeyRouteRewritePlugin", HotKeyRouteRewriteProxyPlugin.class, 20000, 0),
    // Used for hot key caching (only supports GET command)
    HOT_KEY_CACHE_PLUGIN("hotKeyCachePlugin", HotKeyCacheProxyPlugin.class, 10000, Integer.MIN_VALUE + 10000),

    // Used to monitor large keys
    BIG_KEY_PLUGIN("bigKeyPlugin", BigKeyProxyPlugin.class, 0, 0),
    // Used for cache double deletion (only intercepts DEL command)
    DELAY_DOUBLE_DELETE_PLUGIN("delayDoubleDeletePlugin", DelayDoubleDeleteProxyPlugin.class, 0, 0),
    // Used for custom dual-write rules (key-level)
    MULTI_WRITE_PLUGIN("multiWritePlugin", MultiWriteProxyPlugin.class, 0, 0),

    // Used for key/value conversion
    CONVERTER_PLUGIN("converterPlugin", ConverterProxyPlugin.class, Integer.MIN_VALUE, Integer.MAX_VALUE),
    ;
    private final String alias;
    private final Class<? extends ProxyPlugin> clazz;
    private final int requestOrder;
    private final int replyOrder;

    BuildInProxyPluginEnum(String alias, Class<? extends ProxyPlugin> clazz, int requestOrder, int replyOrder) {
        this.alias = alias;
        this.clazz = clazz;
        this.requestOrder = requestOrder;
        this.replyOrder = replyOrder;
    }
}
```
Built-in plugin list:
* MonitorProxyPlugin, used to monitor command request QPS and response, as well as slow queries, see: [monitor-plugin](monitor-plugin.md)
* BigKeyProxyPlugin, used to monitor large keys, see: [big-key](big-key.md)
* HotKeyProxyPlugin, used to monitor hot keys, see: [hot-key](hot-key.md)
* HotKeyCacheProxyPlugin, used for hot key caching, supports GET command, see: [hot-key-cache](hot-key-cache.md)
* ConverterProxyPlugin, used for key/value conversion, such as encryption/decryption, key namespace, etc., see: [converter](converter.md)
* MultiWriteProxyPlugin, used for custom dual-write (can be at key level), see: [multi-write](multi-write.md)
* DelayDoubleDeleteProxyPlugin, used for transparent cache double deletion (only intercepts DEL command), see: [delay-double-delete](delay-double-delete.md)
* TroubleTrickKeysProxyPlugin, used to temporarily intercept certain commands for problematic keys, see: [trouble-trick-keys](trouble-trick-keys.md)
* RateLimitProxyPlugin, used for rate control, supports tenant-level control, see: [rate-limit](rate-limit.md)
* DynamicRateLimitProxyPlugin, used for rate control, supports tenant-level control, configuration hosted by camellia-dashboard, see: [dynamic-rate-limit](dynamic-rate-limit.md)
* IPCheckProxyPlugin, used to control client access, supports IP blacklist/whitelist, see: [ip-checker](ip-checker.md)
* DynamicIpCheckProxyPlugin, used to control client access, supports IP blacklist/whitelist, configuration hosted by camellia-dashboard, see: [dynamic-ip-checker](dynamic-ip-checker.md)
* CommandDisableProxyPlugin, used to disable certain commands, see: [command-disable](command-disable.md)
* HotKeyRouteRewriteProxyPlugin, covers the functionality of HotKeyProxyPlugin, used to monitor hot keys and forward to custom routes, see: [hot-key-route-rewrite](hot-key-route-rewrite.md)
* ReadOnlyProxyPlugin, when enabled, only accepts read commands, see: [read-only](read-only.md)

### How to Modify Default Execution Order of Built-in Plugins
In camellia-redis-proxy.properties or application.yml's config, configure the following key-value-config:
```
## Modify request order
{alias}.build.in.plugin.request.order=10
## Modify reply order
{alias}.build.in.plugin.reply.order=10
```

### Other Plugins Provided by Camellia (Not Built-in, Require Additional Maven Dependencies)
* MqMultiWriteProducerProxyPlugin, used for asynchronous dual-write using MQ, see: [mq-multi-write](mq-multi-write.md)
* HotKeyMonitorPlugin/HotKeyCachePlugin, hot key monitoring/caching based on hot-key-sdk/hot-key-server, see: [hot-key-cache-extension](hot-key-cache-extension.md)

### Custom Plugins
Implement the ProxyPlugin interface and configure the full class name of the implementation class to `proxy.plugin.list`
