package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTransferUtil;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Created by caojiajun on 2021/6/25
 */
public class PasswordMaskUtils {

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
            url = url.substring(0, i+3) + userNameAndPassword + url.substring(j);
            return url;
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
            int i = addr.lastIndexOf("@");
            if (i == 0) return addr;
            String substring = addr.substring(0, i);
            int k = substring.indexOf(":");
            if (k != -1) {
                substring = substring.substring(0, k) + ":" + maskStr(substring.length() - k - 1);
            } else {
                substring = maskStr(substring.length());
            }
            return substring + addr.substring(i);
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
