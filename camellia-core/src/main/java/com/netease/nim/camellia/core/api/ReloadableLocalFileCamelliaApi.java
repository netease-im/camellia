package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.tools.utils.FileUtils;
import com.netease.nim.camellia.tools.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2021/3/31
 */
public class ReloadableLocalFileCamelliaApi implements CamelliaApi {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableLocalFileCamelliaApi.class);

    private final String filePath;
    private final ResourceTableChecker checker;
    private String md5;
    private ResourceTable resourceTable;

    public ReloadableLocalFileCamelliaApi(String filePath) {
        this(filePath, null);
    }

    public ReloadableLocalFileCamelliaApi(String filePath, ResourceTableChecker checker) {
        this.filePath = filePath;
        this.checker = checker;
        checkAndReload(true);
        if (resourceTable == null) {
            throw new IllegalArgumentException("resource table is null, file-path = " + filePath);
        }
    }

    private synchronized void checkAndReload(boolean throwError) {
        try {
            FileUtils.FileInfo fileInfo = FileUtils.readByFilePath(filePath);
            if (fileInfo == null) return;
            if (fileInfo.getFileContent() == null) return;
            String content = fileInfo.getFileContent().trim();
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(content);
            if (checker != null) {
                boolean pass = checker.check(resourceTable);
                if (!pass) {
                    logger.warn("reload fail for check no pass, filePath = {}, content = {}", filePath, content);
                    return;
                }
            }
            String md5 = MD5Util.md5(content);
            if (Objects.equals(md5, this.md5)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not modify, filePath = {}", filePath);
                }
            } else {
                logger.info("reload success, filePath = {}, last.md5 = {}, last.resourceTable = {}," +
                                " current.md5 = {}, current.resourceTable = {}", this.filePath, this.md5,
                        this.resourceTable == null ? null : ReadableResourceTableUtil.readableResourceTable(this.resourceTable),
                        md5, ReadableResourceTableUtil.readableResourceTable(resourceTable));
                this.resourceTable = resourceTable;
                this.md5 = md5;
            }
        } catch (Exception e) {
            logger.error("checkAndReload error, filePath = {}", filePath, e);
            if (throwError) throw e;
        }
    }

    @Override
    public CamelliaApiResponse getResourceTable(Long bid, String bgroup, String md5) {
        checkAndReload(false);
        CamelliaApiResponse response = new CamelliaApiResponse();
        if (md5 != null && md5.equals(this.md5)) {
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        response.setCode(CamelliaApiCode.SUCCESS.getCode());
        response.setResourceTable(resourceTable);
        response.setMd5(this.md5);
        return response;
    }

    @Override
    public CamelliaApiV2Response getResourceTableV2(Long bid, String bgroup, String md5) {
        return ResourceTableUtil.toV2Response(getResourceTable(bid, bgroup, md5));
    }

    @Override
    public boolean reportStats(ResourceStats resourceStats) {
        return true;
    }
}
