package com.netease.nim.camellia.feign.route;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/2
 */
public class CamelliaDashboardFeignResourceTableUpdater extends FeignResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDashboardFeignResourceTableUpdater.class);
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory(CamelliaDashboardFeignResourceTableUpdater.class, true));

    private final CamelliaApi camelliaApi;
    private final long bid;
    private final String bgroup;
    private String md5;

    public CamelliaDashboardFeignResourceTableUpdater(CamelliaApi camelliaApi, long bid, String bgroup, long checkIntervalMillis) {
        this.camelliaApi = camelliaApi;
        this.bid = bid;
        this.bgroup = bgroup;
        executorService.scheduleAtFixedRate(this::refresh, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ResourceTable getResourceTable() {
        CamelliaApiResponse response = camelliaApi.getResourceTable(bid, bgroup, null);
        return response.getResourceTable();
    }

    private void refresh() {
        try {
            CamelliaApiResponse response = camelliaApi.getResourceTable(bid, bgroup, md5);
            if (response.getCode() == CamelliaApiCode.NOT_MODIFY.getCode()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not modify, bid = {}, bgroup = {}, md5 = {}", bid, bgroup, md5);
                }
                return;
            }
            if (response.getCode() != CamelliaApiCode.SUCCESS.getCode()) {
                logger.error("api code = {}, bid = {}, bgroup = {}", response.getCode(), bid, bgroup);
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("getOrUpdate resourceTable success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}",
                        bid, bgroup, response.getMd5(), ReadableResourceTableUtil.readableResourceTable(response.getResourceTable()));
            }
            if (response.getResourceTable() != null) {
                invokeUpdateResourceTable(response.getResourceTable());
                this.md5 = response.getMd5();
            }
        } catch (Exception e) {
            logger.error("refresh error, bid = {}, bgroup = {}", bid, bgroup, e);
        }
    }
}
