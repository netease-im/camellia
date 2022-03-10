package com.netease.nim.camellia.feign.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;

import java.util.Set;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignResourceUtils {

    public static void checkResourceTable(ResourceTable resourceTable) {
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource resource : allResources) {
            parseResourceByUrl(resource);
        }
    }

    public static Resource parseResourceByUrl(Resource resource) {
        String url = resource.getUrl();
        if (url.startsWith(FeignType.Feign.getPrefix())) {
            String feignUrl = url.substring(FeignType.Feign.getPrefix().length());
            return new FeignResource(feignUrl);
        } else if (url.startsWith(FeignType.FeignDiscovery.getPrefix())) {
            String discoveryUrl = url.substring(FeignType.FeignDiscovery.getPrefix().length());
            return new FeignDiscoveryResource(discoveryUrl);
        } else {
            throw new IllegalArgumentException("not feign resource");
        }
    }
}
