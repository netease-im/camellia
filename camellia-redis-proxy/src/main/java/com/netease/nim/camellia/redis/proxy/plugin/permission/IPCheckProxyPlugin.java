package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.permission.model.IpCheckInfo;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个支持设置客户端ip校验的Plugin，支持黑名单和白名单两种模式
 * 配置存储于ProxyDynamicConf(camellia-redis-proxy.properties)中，支持动态修改，也支持根据bid/bgroup设置不同的策略
 *
 * 黑名单示例（支持ip，也支持网段，逗号分隔）：
 * ip.check.mode=1
 * ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
 *
 * 白名单示例（支持ip，也支持网段，逗号分隔）：
 * ip.check.mode=2
 * ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
 *
 * 根据bid/bgroup设置不同的策略：
 * 黑名单示例（表示bid=1,bgroup=default的黑名单配置）：
 * 1.default.ip.check.mode=1
 * 1.default.ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
 *
 * 白名单示例（表示bid=1,bgroup=default的白名单配置）：
 * 1.default.ip.check.mode=2
 * 1.default.ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
 * Created by caojiajun on 2022/9/13
 */
public class IPCheckProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(IPCheckProxyPlugin.class);

    private static final ProxyPluginResponse FORBIDDEN = new ProxyPluginResponse(false, "ip forbidden");
    private final Map<String, IpCheckInfo> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedHashMap<String, Boolean> checkCache = new ConcurrentLinkedHashMap.Builder<String, Boolean>()
            .initialCapacity(1000)
            .maximumWeightedCapacity(10000)
            .build();

    @Override
    public void init(ProxyBeanFactory factory) {
        ProxyDynamicConf.registerCallback(() -> {
            try {
                cache.clear();
                checkCache.clear();
            } catch (Exception e) {
                logger.error("cache clear error", e);
            }
        });
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.IP_CHECKER_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.IP_CHECKER_PLUGIN.getReplyOrder();
            }
        };
    }

    /**
     * when proxy configure in multi-tenancy
     * first commands like AUTH/HELLO will check ip of global config
     * other commands will check bid/brgoup config
     *
     * @param request the context of the request command
     * @return ProxyPluginResponse
     */
    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            CommandContext commandContext = command.getCommandContext();
            String consid = command.getChannelInfo().getConsid();
            String key = null;
            if (consid != null) {
                key = consid + "|" + commandContext.getBid() + "|" + commandContext.getBgroup();
            }
            if (key != null) {
                Boolean checkCache = this.checkCache.get(key);
                if (checkCache != null) {
                    if (checkCache) {
                        return ProxyPluginResponse.SUCCESS;
                    } else {
                        return FORBIDDEN;
                    }
                }
            }
            SocketAddress clientSocketAddress = commandContext.getClientSocketAddress();
            if (clientSocketAddress instanceof InetSocketAddress) {
                String ip = ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
                if (ip != null) {
                    if (!checkIp(commandContext.getBid(), commandContext.getBgroup(), ip)) {
                        ErrorLogCollector.collect(IPCheckProxyPlugin.class, "ip = " + ip + " check fail");
                        if (key != null) {
                            this.checkCache.put(key, Boolean.FALSE);
                        }
                        return FORBIDDEN;
                    }
                }
            }
            if (key != null) {
                this.checkCache.put(key, Boolean.TRUE);
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(IPCheckProxyPlugin.class, "ip check error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }

    public boolean checkIp(Long bid, String bgroup, String ip) {
        String key = bid + "|" + bgroup;
        IpCheckInfo ipCheckInfo = cache.get(key);
        if (ipCheckInfo == null) {
            int mode = ProxyDynamicConf.getInt("ip.check.mode", bid, bgroup, 0);
            IpCheckMode ipCheckMode = IpCheckMode.getByValue(mode);
            ipCheckInfo = new IpCheckInfo(ipCheckMode);
            if (ipCheckMode == IpCheckMode.WHITE) {
                load(ipCheckInfo, ProxyDynamicConf.getString("ip.white.list", bid, bgroup, null));
            } else if (ipCheckMode == IpCheckMode.BLACK) {
                load(ipCheckInfo, ProxyDynamicConf.getString("ip.black.list", bid, bgroup, null));
            }
            cache.put(key, ipCheckInfo);
        }
        return ipCheckInfo.check(ip);
    }

    private void load(IpCheckInfo ipCheckInfo, String conf) {
        if (conf != null) {
            String[] split = conf.split(",");
            for (String str : split) {
                try {
                    if (str.contains("/")) {
                        //网段
                        String[] subSplit = str.split("/");
                        if (subSplit.length == 2) {
                            ipCheckInfo.addIpWithMask(subSplit[0], subSplit[1]);
                        }
                    } else {
                        //单ip
                        ipCheckInfo.addIp(str);
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(IPCheckProxyPlugin.class, "load ip black/white list error, conf = " + str, e);
                }
            }
        }
    }
}
