package com.netease.nim.camellia.dashboard.daowrapper;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.dao.TableDao;
import com.netease.nim.camellia.dashboard.model.ValidFlag;
import com.netease.nim.camellia.dashboard.model.Table;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.core.util.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Service
public class TableDaoWrapper {

    private static final String tag = "camellia_table";

    @Autowired
    private TableDao tableDao;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private DashboardProperties dashboardProperties;

    public int create(Table table) {
        Table save = tableDao.save(table);
        String cacheKey = CacheUtil.buildCacheKey(tag, save.getTid());
        template.del(cacheKey);
        table.setTid(save.getTid());
        return 1;
    }

    public Table get(long tid) {
        String cacheKey = CacheUtil.buildCacheKey(tag, tid);
        String value = template.get(cacheKey);
        if (value != null && value.length() > 0) {
            return JSONObject.parseObject(value, Table.class);
        }
        Optional<Table> tableOptional = tableDao.findById(tid);
        if (!tableOptional.isPresent()) {
            return null;
        }
        Table table = tableOptional.get();
        value = JSONObject.toJSONString(table);
        template.setex(cacheKey, dashboardProperties.getDaoCacheExpireSeconds(), value);
        return table;
    }

    public int delete(Table table) {
        String cacheKey = CacheUtil.buildCacheKey(tag, table.getTid());
        template.del(cacheKey);
        table.setValidFlag(ValidFlag.NOT_VALID.getValue());
        table.setUpdateTime(System.currentTimeMillis());
        tableDao.save(table);
        return 1;
    }

    public List<Table> getList() {
        return tableDao.findAll();
    }

    public int save(Table table) {
        String cacheKey = CacheUtil.buildCacheKey(tag, table.getTid());
        template.del(cacheKey);
        tableDao.save(table);
        return 1;
    }

    public List<Table> getPageValidFlagInfo(Integer validFlag, String info,Long tid,String detail, Integer currentNum, Integer pageSize) {
        return tableDao.getPageValidFlagInfo(validFlag,info,tid,detail,currentNum,pageSize);
    }

    public Integer countPageValidFlagInfo(Integer validFlag, String info,Long tid,String detail) {
        return tableDao.countPageValidFlagInfo(validFlag,info,tid,detail);
    }
}
