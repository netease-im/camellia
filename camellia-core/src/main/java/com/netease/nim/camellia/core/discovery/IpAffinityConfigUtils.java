camellia-core/src/main/java/com/netease/nim/camellia/core/discovery/IpAffinityConfigUtils.javapackage com.netease.nim.camellia.core.discovery;

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

    private static final ConcurrentLinkedHashMap<String, IpAffinityConfig> cache = new ConcurrentLinkedHashMap.Builder<String, IpAffinityConfig>()
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

    public static boolean match(IpAffinityConfig config, String targetIp) {
        return match(config, host, targetIp);
    }

    public static boolean match(IpAffinityConfig config, String host, String targetIp) {
        try {
            IpAffinityConfig.Type type = config.getType();
            if (type == null) {
                return true;
            }
            List<IpAffinityConfig.Config> configList = config.getConfigList();
            if (configList == null || configList.isEmpty()) {
                return true;
            }
            for (IpAffinityConfig.Config affinityconfig : configList) {
                if (affinityconfig.getSource().match(host) && affinityconfig.getTarget().match(targetIp)) {
                    return type == IpAffinityConfig.Type.affinity;
                }
            }
            return type != IpAffinityConfig.Type.affinity;
        } catch (Exception e) {
            return true;
        }
    }

    public static IpAffinityConfig parse(String configStr) {
        try {
            IpAffinityConfig config = cache.get(configStr);
            if (config != null) {
                return config;
            }
            config = new IpAffinityConfig();
            JSONObject jsonObject = JSONObject.parseObject(configStr);
            String type = jsonObject.getString("type");
            if (type.equals(IpAffinityConfig.Type.affinity.name())) {
                config.setType(IpAffinityConfig.Type.affinity);
            } else if (type.equals(IpAffinityConfig.Type.anti_affinity.name())) {
                config.setType(IpAffinityConfig.Type.anti_affinity);
            }
            JSONArray array = jsonObject.getJSONArray("config");
            List<IpAffinityConfig.Config> list = new ArrayList<>();
            for (Object o : array) {
                JSONObject json = ((JSONObject) o);
                String source = json.getString("source");
                String target = json.getString("target");
                IpAffinityConfig.Config ipAffinityConfig = new IpAffinityConfig.Config();
                ipAffinityConfig.setSource(initIPMatcher(source));
                ipAffinityConfig.setTarget(initIPMatcher(target));
                list.add(ipAffinityConfig);
            }
            config.setConfigList(list);
            cache.put(configStr, config);
            return config;
        } catch (Exception e) {
            cache.put(configStr, new IpAffinityConfig());
            return new IpAffinityConfig();
        }
    }

    private static IPMatcher initIPMatcher(String str) {
        String[] split = str.split("/");
        String ip = split[0];
        String mask = split[1];
        return new IPMatcher(ip, mask);
    }
}
