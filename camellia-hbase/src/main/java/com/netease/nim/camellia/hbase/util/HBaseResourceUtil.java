package com.netease.nim.camellia.hbase.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.resource.HBaseResource;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResourceUtil {

    public static HBaseResource parseResourceByUrl(Resource resource) {
        if (!resource.getUrl().startsWith(HBaseResource.prefix)) {
            throw new IllegalArgumentException("not start with '" + HBaseResource.prefix + "'");
        }
        String substring = resource.getUrl().substring(HBaseResource.prefix.length());
        if (!substring.contains("/")) {
            throw new IllegalArgumentException("miss '/'");
        }
        String[] split = substring.split("/");
        HBaseResource hBaseResource = new HBaseResource(split[0], "/" + split[1]);
        String zk = hBaseResource.getZk();
        String zkParent = hBaseResource.getZkParent();
        if (zk == null || zk.length() == 0) {
            throw new IllegalArgumentException("zk is empty");
        }
        if (zkParent == null || zkParent.length() == 0) {
            throw new IllegalArgumentException("zkParent is empty");
        }
        return hBaseResource;
    }
}
