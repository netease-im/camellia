package com.netease.nim.camellia.core.discovery;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.tools.utils.IPMatcher;
import com.netease.nim.camellia.tools.utils.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/3/25
 */
public class IpAffinityConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(IpAffinityConfigUtils.class);

    private static final ConcurrentLinkedHashMap<String, List<IpAffinityConfig>> cache = new ConcurrentLinkedHashMap.Builder<String, List<IpAffinityConfig>>()
            .initialCapacity(1000)
            .maximumWeightedCapacity(1000)
            .build();

    private static String host;
    static {
        try {
            InetAddress address = InetUtils.findFirstNonLoopbackAddress();
            if (address != null) {
                host = address.getHostAddress();
            } else {
                host = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            host = "unknown";
        }
    }

    public static boolean match(String config, String targetIp) {
        try {
            List<IpAffinityConfig> list = cache.get(config);
            if (list == null) {
                list = init(config);
                cache.put(config, list);
            }
            if (list.isEmpty()) {
                return true;
            }
            for (IpAffinityConfig affinityConfig : list) {
                if (affinityConfig.getSource().match(host) && affinityConfig.getTarget().match(targetIp)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static List<IpAffinityConfig> init(String config) {
        try {
            List<IpAffinityConfig> list = new ArrayList<>();
            JSONArray array = JSONArray.parseArray(config);
            for (Object o : array) {
                JSONObject json = ((JSONObject) o);
                String source = json.getString("source");
                String target = json.getString("target");
                IpAffinityConfig ipAffinityConfig = new IpAffinityConfig();
                ipAffinityConfig.setSource(initIPMatcher(source));
                ipAffinityConfig.setTarget(initIPMatcher(target));
                list.add(ipAffinityConfig);
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static IPMatcher initIPMatcher(String str) {
        String[] split = str.split("/");
        String ip = split[0];
        String mask = split[1];
        return new IPMatcher(ip, mask);
    }
}
