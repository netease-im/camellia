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
            .initialCapacity(128)
            .maximumWeightedCapacity(128)
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

    public static boolean match(List<IpAffinityConfig> list, String targetIp) {
        try {
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

    public static List<IpAffinityConfig> parse(String config) {
        try {
            List<IpAffinityConfig> list = cache.get(config);
            if (list != null) {
                return list;
            }
            list = new ArrayList<>();
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
            cache.put(config, list);
            return list;
        } catch (Exception e) {
            cache.put(config, new ArrayList<>());
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
