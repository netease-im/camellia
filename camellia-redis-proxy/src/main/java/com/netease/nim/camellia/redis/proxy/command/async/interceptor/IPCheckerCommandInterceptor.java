package com.netease.nim.camellia.redis.proxy.command.async.interceptor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.discovery.common.IPMatcher;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 一个设置客户端ip校验的拦截器，支持黑名单和白名单两种模式
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
 *
 * Created by caojiajun on 2022/5/9
 */
public class IPCheckerCommandInterceptor implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(IPCheckerCommandInterceptor.class);

    private static final CommandInterceptResponse FORBIDDEN = new CommandInterceptResponse(false, "ip forbidden");

    private final Map<String, IpCheckInfo> cache = new HashMap<>();

    public IPCheckerCommandInterceptor() {
        ProxyDynamicConf.registerCallback(() -> {
            try {
                cache.clear();
            } catch (Exception e) {
                logger.error("cache clear error", e);
            }
        });
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        try {
            CommandContext commandContext = command.getCommandContext();
            SocketAddress clientSocketAddress = commandContext.getClientSocketAddress();
            if (clientSocketAddress instanceof InetSocketAddress) {
                String ip = ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
                if (ip != null) {
                    if (!checkIp(commandContext.getBid(), commandContext.getBgroup(), ip)) {
                        ErrorLogCollector.collect(IPCheckerCommandInterceptor.class, "ip = " + ip + " check fail");
                        return FORBIDDEN;
                    }
                }
            }
            return CommandInterceptResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(IPCheckerCommandInterceptor.class, "ip check error", e);
            return CommandInterceptResponse.SUCCESS;
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
                    ErrorLogCollector.collect(IPCheckerCommandInterceptor.class, "load ip black/white list error, conf = " + str, e);
                }
            }
        }
    }

    private enum IpCheckMode {
        BLACK(1),
        WHITE(2),
        UNKNOWN(0),
        ;
        private final int value;

        IpCheckMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static IpCheckMode getByValue(int value) {
            for (IpCheckMode mode : IpCheckMode.values()) {
                if (mode.value == value) {
                    return mode;
                }
            }
            return UNKNOWN;
        }
    }

    private static class IpCheckInfo {
        private final IpCheckMode mode;
        private final Set<IPMatcher> ipMatcherSet = new HashSet<>();
        private final Set<String> ipSet = new HashSet<>();

        public IpCheckInfo(IpCheckMode mode) {
            this.mode = mode;
        }

        public void addIp(String ip) {
            ipSet.add(ip);
        }

        public void addIpWithMask(String ip, String mask) {
            ipMatcherSet.add(new IPMatcher(ip, mask));
        }

        public boolean check(String ip) {
            if (mode == IpCheckMode.WHITE) {
                if (!ipSet.isEmpty()) {
                    for (String whiteIp : ipSet) {
                        if (ip.equals(whiteIp)) {
                            return true;
                        }
                    }
                }
                if (!ipMatcherSet.isEmpty()) {
                    for (IPMatcher ipMatcher : ipMatcherSet) {
                        if (ipMatcher.match(ip)) {
                            return true;
                        }
                    }
                }
                return false;
            } else if (mode == IpCheckMode.BLACK) {
                if (!ipSet.isEmpty()) {
                    for (String whiteIp : ipSet) {
                        if (ip.equals(whiteIp)) {
                            return false;
                        }
                    }
                }
                if (!ipMatcherSet.isEmpty()) {
                    for (IPMatcher ipMatcher : ipMatcherSet) {
                        if (ipMatcher.match(ip)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return true;
        }
    }
}
