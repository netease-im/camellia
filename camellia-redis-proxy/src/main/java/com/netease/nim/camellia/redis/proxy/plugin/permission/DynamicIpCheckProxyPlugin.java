package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.api.*;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.permission.model.IpCheckInfo;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
public class DynamicIpCheckProxyPlugin implements ProxyPlugin {
    private static final Logger logger = LoggerFactory.getLogger(DynamicIpCheckProxyPlugin.class);
    protected static final ProxyPluginResponse FORBIDDEN = new ProxyPluginResponse(false, "ip forbidden");

    // This key is applied for all bid and bgroup if bid and bgroup is null
    public static final String DEFAULT_KEY = IpCheckerUtil.buildIpCheckerKey(-1L, "default");

    private final ConcurrentLinkedHashMap<String, Boolean> checkCache = new ConcurrentLinkedHashMap.Builder<String, Boolean>()
            .initialCapacity(1000)
            .maximumWeightedCapacity(10000)
            .build();

    protected final Map<String, IpCheckInfo> cache = new ConcurrentHashMap<>();
    private String md5;

    private CamelliaMiscApi api;

    @Override
    public void init(ProxyBeanFactory factory) {
        try {
            //proxy route conf may not configured by camellia-dashboard, so init CamelliaApi independent
            String url = ProxyDynamicConf.getString("camellia.dashboard.url", null);
            int connectTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.connectTimeoutMillis", 10000);
            int readTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.readTimeoutMillis", 60000);
            Map<String, String> headerMap = new HashMap<>();
            String string = ProxyDynamicConf.getString("camellia.dashboard.headerMap", "{}");
            JSONObject json = JSONObject.parseObject(string);
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                headerMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            api = CamelliaApiUtil.initMiscApi(url, connectTimeoutMillis, readTimeoutMillis, headerMap);
            int seconds = ProxyDynamicConf.getInt("dynamic.ip.check.plugin.config.update.interval.seconds", 5);
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
        checkCache.clear();
    }

    /**
     * Fetch ip checker config from camellia-dashboard via http request
     */
    private List<IpCheckerDto> fetchIpCheckerData() {
        try {
            DataWithMd5Response<List<IpCheckerDto>> response = api.getIpCheckerList(md5);
            if (response != null && CamelliaApiCode.SUCCESS.getCode() == response.getCode() && response.getData() != null) {
                String newMd5 = response.getMd5();
                if (hasChange(newMd5)) {
                    this.md5 = newMd5;
                    return response.getData();
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(DynamicIpCheckProxyPlugin.class, "cannot fetch ip checker config from camellia-dashboard", e);
        }
        return null;
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
                        ErrorLogCollector.collect(DynamicIpCheckProxyPlugin.class, "ip = " + ip + " check fail");
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
                    ErrorLogCollector.collect(DynamicIpCheckProxyPlugin.class, "load ip black/white list error, conf = " + ip, e);
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
