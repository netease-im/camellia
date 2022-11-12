package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.permission.model.IpCheckInfo;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.IpCheckerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
public class DynamicIpCheckProxyPlugin implements ProxyPlugin {
    private static final Logger logger = LoggerFactory.getLogger(DynamicIpCheckProxyPlugin.class);
    protected static final ProxyPluginResponse FORBIDDEN = new ProxyPluginResponse(false, "ip forbidden");
    protected static final String PROXY_PLUGIN_UPDATE_INTERVAL_SECONDS_KEY = "proxy.plugin.update.interval.seconds";

    // This key is applied for all bid and bgroup if bid and bgroup is null
    public static final String DEFAULT_KEY = IpCheckerUtil.buildIpCheckerKey(0L, "0");

    protected final Map<String, IpCheckInfo> cache = new HashMap<>();
    private String md5;

    private CamelliaApi apiService;

    @Override
    public void init(ProxyBeanFactory factory) {
        try {
            // FIXME: I don't know how to get the apiService from factory. So I try getting it from AsyncCamelliaRedisTemplateChooser
            // And I think it's not a good idea to get it from AsyncCamelliaRedisTemplateChooser. But this is the only way I can think of.
            this.apiService = AsyncCamelliaRedisTemplateChooser.apiService;
            int seconds = ProxyDynamicConf.getInt(PROXY_PLUGIN_UPDATE_INTERVAL_SECONDS_KEY, 10);
            ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
            reload();
        } catch (Exception e) {
            logger.error("init dynamic IP Check Proxy error", e);
        }
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.DYNAMIC_IP_CHECKER_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.DYNAMIC_IP_CHECKER_PLUGIN.getReplyOrder();
            }
        };
    }

    private void reload() {
        List<IpCheckerDto> ipCheckers = fetchIpCheckerData();
        if (ipCheckers == null) {
            return;
        }
        Map<String, IpCheckInfo> newData = ipCheckers.stream()
                .collect(
                        Collectors.toMap(
                                ipChecker -> IpCheckerUtil.buildIpCheckerKey(ipChecker.getBid(), ipChecker.getBgroup()),
                                ipChecker -> {
                                    IpCheckInfo ipCheckInfo = new IpCheckInfo(ipChecker.getMode());
                                    load(ipCheckInfo, ipChecker.getIpList());
                                    return ipCheckInfo;
                                }));
        cache.clear();
        cache.putAll(newData);
    }

    /**
     * Fetch ip checker config from camellia-dashboard via http request
     */
    private List<IpCheckerDto> fetchIpCheckerData() {
        try {
            DataWithMd5Response<List<IpCheckerDto>> response = apiService.getIpCheckerList(md5);
            if (response != null && CamelliaApiCode.SUCCESS.getCode() == response.getCode() && response.getData() != null) {
                String newMd5 = response.getMd5();
                if (hasChange(newMd5)) {
                    this.md5 = newMd5;
                    return response.getData();
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(IPCheckProxyPlugin.class, "cannot fetch ip checker config from camellia-dashboard", e);
        }
        return null;
    }


    /**
     * @param request the context of the request command
     * @return ProxyPluginResponse
     */
    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            CommandContext commandContext = command.getCommandContext();
            SocketAddress clientSocketAddress = commandContext.getClientSocketAddress();
            if (clientSocketAddress instanceof InetSocketAddress) {
                String ip = ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
                if (ip != null) {
                    if (!checkIp(commandContext.getBid(), commandContext.getBgroup(), ip)) {
                        ErrorLogCollector.collect(DynamicIpCheckProxyPlugin.class, "ip = " + ip + " check fail");
                        return FORBIDDEN;
                    }
                }
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(DynamicIpCheckProxyPlugin.class, "ip check error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }

    /**
     * Check Ip whether is allowed
     * Load data from cache first by key = bid|bgroup
     * If not found, load data from cache by key = DEFAULT_KEY (global config)
     *
     * @param bid    bid
     * @param bgroup bgroup
     * @param ip     ip
     * @return is accessible ip
     */
    public boolean checkIp(Long bid, String bgroup, String ip) {
        String key = IpCheckerUtil.buildIpCheckerKey(bid, bgroup);
        IpCheckInfo ipCheckInfo = cache.get(key);
        if (ipCheckInfo != null) {
            return ipCheckInfo.check(ip);
        }
        IpCheckInfo globalIpCheckInfo = cache.get(DEFAULT_KEY);
        if (globalIpCheckInfo != null) {
            cache.put(key, globalIpCheckInfo);
            return globalIpCheckInfo.check(ip);
        }
        return true;
    }

    /**
     * @param ipCheckInfo ipCheckInfo
     * @param ipList      configuration set string
     */
    private void load(IpCheckInfo ipCheckInfo, Set<String> ipList) {
        if (ipList != null && !ipList.isEmpty()) {
            for (String ip : ipList) {
                try {
                    if (ip.contains("/")) {
                        // network segment
                        String[] subSplit = ip.split("/");
                        if (subSplit.length == 2) {
                            ipCheckInfo.addIpWithMask(subSplit[0], subSplit[1]);
                        }
                    } else {
                        // single ip
                        ipCheckInfo.addIp(ip);
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(IPCheckProxyPlugin.class, "load ip black/white list error, conf = " + ip, e);
                }
            }
        }
    }

    /**
     * Check if config has been changed
     *
     * @param newMd5 new md5
     * @return true if config has been changed
     */
    private boolean hasChange(String newMd5) {
        return this.md5 == null || (newMd5 != null && !this.md5.equals(newMd5));
    }

}
