package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2021/4/2
 */
public class GlobalRedisProxyEnv {

    private static final Logger logger = LoggerFactory.getLogger(GlobalRedisProxyEnv.class);

    public static AsyncCamelliaRedisTemplateChooser chooser;

    public static int bossThread;
    public static EventLoopGroup bossGroup;

    public static int workThread;
    public static EventLoopGroup workGroup;

    public static int port;
    public static int cport;
    public static int consolePort;

    public static ProxyDiscoveryFactory discoveryFactory;

    private static final Set<Runnable> callbackSet = new HashSet<>();
    public static synchronized void addStartOkCallback(Runnable callback) {
        callbackSet.add(callback);
    }

    public static void invokeCallback() {
        for (Runnable runnable : callbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("callback error", e);
            }
        }
    }
}
