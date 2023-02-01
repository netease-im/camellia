package com.netease.nim.camellia.hbase.util;

import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.hbase.resource.HBaseResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResourceUtil {

    public static final ResourceTableChecker HBaseResourceTableChecker = resourceTable -> {
        try {
            checkResourceTable(resourceTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    };

    public static void checkResourceTable(ResourceTable resourceTable) {
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource allResource : allResources) {
            HBaseResourceUtil.parseResourceByUrl(allResource);
        }
    }

    public static HBaseResource parseResourceByUrl(Resource resource) {
        if (!resource.getUrl().startsWith(HBaseResource.prefix)) {
            throw new IllegalArgumentException("not start with '" + HBaseResource.prefix + "'");
        }
        String substring = resource.getUrl().substring(HBaseResource.prefix.length());
        if (!substring.contains("/")) {
            throw new IllegalArgumentException("miss '/'");
        }
        String[] split = substring.split("/");
        String zk = split[0];
        String str = split[1];
        String zkParent;
        String userName = null;
        String password = null;
        if (!str.contains("?")) {
            zkParent = "/" + str;
        } else {
            String[] strings = str.split("\\?");
            zkParent = "/" + strings[0];
            if (strings.length > 1) {
                Map<String, String> params = getParams(strings[1]);
                userName = params.get("userName");
                password = params.get("password");
            }
        }
        HBaseResource hBaseResource = new HBaseResource(zk, zkParent, userName, password);
        if (zk == null || zk.length() == 0) {
            throw new IllegalArgumentException("zk is empty");
        }
        return hBaseResource;
    }

    public static Map<String, String> getParams(String queryString) {
        String[] split1 = queryString.split("&");
        Map<String, String> map = new HashMap<>();
        for (String s : split1) {
            String[] split3 = s.split("=");
            if (split3.length != 2) continue;
            String k = split3[0];
            String v = split3[1];
            map.put(k, v);
        }
        return map;
    }
}
