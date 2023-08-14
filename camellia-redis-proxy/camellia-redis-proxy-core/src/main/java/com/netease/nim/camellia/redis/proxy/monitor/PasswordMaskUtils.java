package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTransferUtil;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelSlavesResource;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/6/25
 */
public class PasswordMaskUtils {

    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    static {
        ExecutorUtils.scheduleAtFixedRate(cache::clear, 1, 1, TimeUnit.HOURS);
    }

    private static boolean isMaskEnable() {
        return ProxyDynamicConf.getBoolean("monitor.data.mask.password.enable", Constants.Server.monitorDataMaskPassword);
    }

    public static String maskResource(Resource resource) {
        if (resource == null) return null;
        return maskResource(resource.getUrl());
    }

    public static String maskResource(String url) {
        if (!isMaskEnable()) return url;
        try {
            String maskUrl = cache.get(url);
            if (maskUrl != null) {
                return maskUrl;
            }
            Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
            if (resource instanceof RedisSentinelResource) {
                String password = ((RedisSentinelResource) resource).getPassword() == null ? null : maskStr(((RedisSentinelResource) resource).getPassword().length());
                String sentinelPassword = ((RedisSentinelResource) resource).getSentinelPassword() == null ? null : maskStr(((RedisSentinelResource) resource).getSentinelPassword().length());
                RedisSentinelResource redisSentinelResource = new RedisSentinelResource(((RedisSentinelResource) resource).getMaster(),
                        ((RedisSentinelResource) resource).getNodes(), ((RedisSentinelResource) resource).getUserName(),
                        password, ((RedisSentinelResource) resource).getDb(), ((RedisSentinelResource) resource).getSentinelUserName(), sentinelPassword);
                cache.put(url, redisSentinelResource.getUrl());
                return redisSentinelResource.getUrl();
            } else if (resource instanceof RedisSentinelSlavesResource) {
                String password = ((RedisSentinelSlavesResource) resource).getPassword() == null ? null : maskStr(((RedisSentinelSlavesResource) resource).getPassword().length());
                String sentinelPassword = ((RedisSentinelSlavesResource) resource).getSentinelPassword() == null ? null : maskStr(((RedisSentinelSlavesResource) resource).getSentinelPassword().length());
                RedisSentinelSlavesResource redisSentinelSlavesResource = new RedisSentinelSlavesResource(((RedisSentinelSlavesResource) resource).getMaster(),
                        ((RedisSentinelSlavesResource) resource).getNodes(), ((RedisSentinelSlavesResource) resource).getUserName(),
                        password, ((RedisSentinelSlavesResource) resource).isWithMaster(), ((RedisSentinelSlavesResource) resource).getDb(),
                        ((RedisSentinelSlavesResource) resource).getSentinelUserName(), sentinelPassword);
                cache.put(url, redisSentinelSlavesResource.getUrl());
                return redisSentinelSlavesResource.getUrl();
            }

            int i = url.indexOf("://");
            int j = url.lastIndexOf("@");
            String userNameAndPassword = url.substring(i + 3, j);
            if (userNameAndPassword.length() != 0) {
                int k = userNameAndPassword.indexOf(":");
                if (k != -1) {
                    userNameAndPassword = userNameAndPassword.substring(0, k) + ":" + maskStr(userNameAndPassword.length() - k - 1);
                } else {
                    userNameAndPassword = maskStr(userNameAndPassword.length());
                }
            }
            maskUrl = url.substring(0, i+3) + userNameAndPassword + url.substring(j);
            cache.put(url, maskUrl);
            return maskUrl;
        } catch (Exception e) {
            return url;
        }
    }

    public static String maskAddrs(Collection<RedisConnectionAddr> addrs) {
        if (addrs == null) return null;
        List<String> list = new ArrayList<>();
        for (RedisConnectionAddr addr : addrs) {
            list.add(maskAddr(addr));
        }
        return list.toString();
    }

    public static String maskAddr(RedisConnectionAddr addr) {
        if (addr == null) return null;
        return maskAddr(addr.getUrl());
    }

    public static String maskAddr(String addr) {
        try {
            if (!isMaskEnable()) return addr;
            String maskAddr = cache.get(addr);
            if (maskAddr != null) {
                return maskAddr;
            }
            int i = addr.lastIndexOf("@");
            if (i == 0) return addr;
            String substring = addr.substring(0, i);
            int k = substring.indexOf(":");
            if (k != -1) {
                substring = substring.substring(0, k) + ":" + maskStr(substring.length() - k - 1);
            } else {
                substring = maskStr(substring.length());
            }
            maskAddr = substring + addr.substring(i);
            cache.put(addr, maskAddr);
            return maskAddr;
        } catch (Exception e) {
            return addr;
        }
    }

    public static ResourceTable maskResourceTable(ResourceTable resourceTable) {
        if (!isMaskEnable()) return resourceTable;
        String readableResourceTable = ReadableResourceTableUtil.readableResourceTable(resourceTable);
        ResourceTable resourceTable1 = ReadableResourceTableUtil.parseTable(readableResourceTable);
        return ResourceTransferUtil.transfer(resourceTable1,
                resource -> RedisResourceUtil.parseResourceByUrl(new Resource(maskResource(resource.getUrl()))));
    }

    public static String maskStr(int len) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<len; i++) {
            builder.append("*");
        }
        return builder.toString();
    }
}
