package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 *
 * Created by caojiajun on 2021/3/31
 */
public class ReloadableLocalFileCamelliaApi implements CamelliaApi {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableLocalFileCamelliaApi.class);

    private final String filePath;
    private final ResourceTableChecker checker;
    private long lastModified;
    private String md5;
    private ResourceTable resourceTable;

    public ReloadableLocalFileCamelliaApi(String filePath) {
        this(filePath, null);
    }

    public ReloadableLocalFileCamelliaApi(String filePath, ResourceTableChecker checker) {
        this.filePath = filePath;
        this.checker = checker;
        checkAndReload(true);
    }

    private synchronized void checkAndReload(boolean throwError) {
        try {
            File file = new File(filePath);
            long lastModified = file.lastModified();
            if (resourceTable == null || lastModified != this.lastModified) {
                this.lastModified = lastModified;
                String content = FileUtil.readFileByPath(filePath);
                if (content == null) return;
                content = content.trim();
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(content);
                if (checker != null) {
                    boolean pass = checker.check(resourceTable);
                    if (!pass) {
                        logger.warn("reload fail for check no pass, filePath = {}, content = {}", filePath, content);
                        return;
                    }
                }
                String md5 = MD5Util.md5(content);
                logger.info("reload success, filePath = {}, last.md5 = {}, last.resourceTable = {}," +
                        " current.md5 = {}, current.resourceTable = {}", this.filePath, this.md5,
                        this.resourceTable == null ? null : ReadableResourceTableUtil.readableResourceTable(this.resourceTable),
                        md5, ReadableResourceTableUtil.readableResourceTable(resourceTable));
                this.resourceTable = resourceTable;
                this.md5 = md5;
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("not modify, filePath = {}", filePath);
                }
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
    public boolean reportStats(ResourceStats resourceStats) {
        return true;
    }
}
