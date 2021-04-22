package com.netease.nim.camellia.hbase.util;

import com.netease.nim.camellia.core.api.ReloadableLocalFileCamelliaApi;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.hbase.resource.HBaseResource;

import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResourceUtil {

    public static final ReloadableLocalFileCamelliaApi.ResourceTableChecker HBaseResourceTableChecker = new ReloadableLocalFileCamelliaApi.ResourceTableChecker() {
        @Override
        public boolean check(ResourceTable resourceTable) {
            try {
                checkResourceTable(resourceTable);
                return true;
            } catch (Exception e) {
                return false;
            }
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
