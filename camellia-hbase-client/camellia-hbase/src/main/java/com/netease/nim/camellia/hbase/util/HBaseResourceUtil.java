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
        if (substring.startsWith("obkv%")) {
            try {
                int index = substring.lastIndexOf("%");
                String obkvParamUrl = substring.substring(5, index);
                Map<String, String> params = getParams(substring.substring(index+1));
                String obkvFullUserName = params.get("obkvFullUserName");
                if (obkvFullUserName == null) {
                    throw new IllegalArgumentException("missing 'obkvFullUserName'");
                }
                String obkvPassword = params.get("obkvPassword");
                if (obkvPassword == null) {
                    throw new IllegalArgumentException("missing 'obkvPassword'");
                }
                String obkvSysUserName = params.get("obkvSysUserName");
                if (obkvSysUserName == null) {
                    throw new IllegalArgumentException("missing 'obkvSysUserName'");
                }
                String obkvSysPassword = params.get("obkvSysPassword");
                if (obkvSysPassword == null) {
                    throw new IllegalArgumentException("missing 'obkvSysPassword'");
                }
                return new HBaseResource(obkvParamUrl, obkvFullUserName, obkvPassword, obkvSysUserName, obkvSysPassword);
            } catch (Exception e) {
                throw new IllegalArgumentException("parse obkv url error", e);
            }
        }
        if (!substring.contains("/")) {
            throw new IllegalArgumentException("miss '/'");
        }
        String[] split = substring.split("/");
        String zk = split[0];
        String str = split[1];
        String zkParent;

        if (zk == null || zk.isEmpty()) {
            throw new IllegalArgumentException("zk is empty");
        }

        if (!str.contains("?")) {
            zkParent = "/" + str;
            return new HBaseResource(zk, zkParent);
        } else {
            String[] strings = str.split("\\?");
            zkParent = "/" + strings[0];
            if (strings.length > 1) {
                Map<String, String> params = getParams(strings[1]);
                String lindormStr = params.get("lindorm");
                boolean lindorm = lindormStr != null && lindormStr.equalsIgnoreCase(Boolean.TRUE.toString());
                if (lindorm) {
                    String userName = params.get("userName");
                    String password = params.get("password");
                    return new HBaseResource(zk, zkParent, userName, password, true);
                }
                return new HBaseResource(zk, zkParent, params);
            } else {
                return new HBaseResource(zk, zkParent);
            }
        }
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
