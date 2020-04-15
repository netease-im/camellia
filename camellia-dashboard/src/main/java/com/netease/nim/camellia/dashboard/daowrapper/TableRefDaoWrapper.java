package com.netease.nim.camellia.dashboard.daowrapper;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.dao.TableRefDao;
import com.netease.nim.camellia.dashboard.model.TableRef;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Service
public class TableRefDaoWrapper {

    private static final String tag = "camellia_table_ref";

    @Autowired
    private TableRefDao mapper;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private DashboardProperties dashboardProperties;

    public int save(TableRef tableRef) {
        TableRef save = mapper.save(tableRef);
        tableRef.setId(save.getId());
        String cacheKey = CacheUtil.buildCacheKey(tag, tableRef.getBid(), tableRef.getBgroup());
        template.del(cacheKey);
        return 1;
    }

    public TableRef getByBidBGroup(long bid, String bgroup) {
        String cacheKey = CacheUtil.buildCacheKey(tag, bid, bgroup);
        String value = template.get(cacheKey);
        if (value != null && value.length() > 0) {
            return JSONObject.parseObject(value, TableRef.class);
        }
        TableRef tableRef = mapper.findByBidAndBgroup(bid, bgroup);
        if (tableRef == null) return null;
        value = JSONObject.toJSONString(tableRef);
        template.setex(cacheKey, dashboardProperties.getDaoCacheExpireSeconds(), value);
        return tableRef;
    }

    public List<TableRef> getByTid(long tid) {
        return mapper.findByTid(tid);
    }

    public List<TableRef> getByBid(long bid) {
        return mapper.findByBid(bid);
    }

    public List<TableRef> getList() {
        return mapper.findAll();
    }
}
