package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2021/7/29
 */
public class LocalDynamicCamelliaApi implements CamelliaApi {

    private static final Logger logger = LoggerFactory.getLogger(LocalDynamicCamelliaApi.class);

    private ResourceTable resourceTable;
    private final ResourceTableChecker checker;
    private String md5;

    public LocalDynamicCamelliaApi(ResourceTable resourceTable) {
        this(resourceTable, null);
    }

    public LocalDynamicCamelliaApi(ResourceTable resourceTable, ResourceTableChecker checker) {
        this.resourceTable = resourceTable;
        if (checker != null) {
            boolean pass = checker.check(resourceTable);
            if (!pass) {
                throw new IllegalArgumentException("resourceTable check fail");
            }
        }
        String json = ReadableResourceTableUtil.readableResourceTable(resourceTable);
        this.md5 = MD5Util.md5(json);
        this.checker = checker;
    }

    public void updateResourceTable(ResourceTable resourceTable) {
        if (resourceTable == null) return;
        if (checker != null) {
            boolean pass = checker.check(resourceTable);
            if (!pass) {
                logger.warn("update ResourceTable fail for check no pass");
                return;
            }
        }
        String json = ReadableResourceTableUtil.readableResourceTable(resourceTable);
        this.resourceTable = resourceTable;
        this.md5 = MD5Util.md5(json);
    }

    @Override
    public CamelliaApiResponse getResourceTable(Long bid, String bgroup, String md5) {
        CamelliaApiResponse response = new CamelliaApiResponse();
        if (md5 != null && md5.equals(this.md5)) {
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        response.setMd5(this.md5);
        response.setResourceTable(this.resourceTable);
        return response;
    }

    @Override
    public boolean reportStats(ResourceStats resourceStats) {
        return true;
    }
}
