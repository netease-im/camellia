package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.model.Table;
import com.netease.nim.camellia.dashboard.model.TableRef;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.ValidFlag;
import com.netease.nim.camellia.redis.toolkit.localcache.LocalCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * Created by caojiajun on 2019/5/17.
 */
@Service
public class ResourceTableService {

    @Autowired
    private TableRefService tableRefService;

    @Autowired
    private TableService tableService;

    @Autowired
    private DashboardProperties dashboardProperties;

    private static final String TAG = "tag";
    private final LocalCache localCache = new LocalCache();

    public CamelliaApiResponse get(long bid, String bgroup, String md5) {
        CamelliaApiResponse response = new CamelliaApiResponse();
        String tableString;
        try {
            tableString = getTableString(bid, bgroup);
        } catch (AppException e) {
            response.setCode(e.getCode());
            return response;
        }
        String newMd5 = MD5Util.md5(tableString);
        if (md5 != null && newMd5.equals(md5)) {
            LogBean.get().addProps("not.modify", true);
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(tableString);
        response.setCode(CamelliaApiCode.SUCCESS.getCode());
        response.setMd5(newMd5);
        response.setResourceTable(resourceTable);
        return response;
    }

    private String getTableString(long bid, String bgroup) {
        String cacheKey = bid + "|" + bgroup;
        String cacheValue = localCache.get(TAG, cacheKey, String.class);
        if (cacheValue != null) {
            return cacheValue;
        }
        TableRef tableRef = tableRefService.getByBidBGroup(bid, bgroup);
        if (tableRef == null) {
            LogBean.get().addProps("bid/group.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "bid/group not exists");
        }
        if (tableRef.getValidFlag() == ValidFlag.NOT_VALID.getValue()) {
            LogBean.get().addProps("bid/group.valid", false);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "bid/group not valid");
        }
        Long tid = tableRef.getTid();
        Table table = tableService.get(tid);
        if (table == null) {
            LogBean.get().addProps("tid.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not exists");
        }
        if (table.getValidFlag() == ValidFlag.NOT_VALID.getValue()) {
            LogBean.get().addProps("tid.valid", false);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not valid");
        }
        String tableString = table.getDetail();
        localCache.put(TAG, cacheKey, tableString, dashboardProperties.getLocalCacheExpireSeconds());
        return tableString;
    }
}
