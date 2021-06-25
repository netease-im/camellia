package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTransferUtil;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;

/**
 *
 * Created by caojiajun on 2021/6/25
 */
public class PasswordMaskUtils {

    public static boolean maskEnable = false;

    public static String maskResource(String url) {
        if (!maskEnable) return url;
        int i = url.indexOf("://");
        int j = url.indexOf("@");
        String password = url.substring(i + 3, j);
        if (password.length() != 0) {
            password = maskStr(password.length());
        }
        url = url.substring(0, i+3) + password + url.substring(j);
        return url;
    }

    public static String maskAddr(String addr) {
        if (!maskEnable) return addr;
        int i = addr.indexOf("@");
        if (i == 0) return addr;
        return maskStr(i) + addr.substring(i);
    }

    public static ResourceTable maskResourceTable(ResourceTable resourceTable) {
        if (!maskEnable) return resourceTable;
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
