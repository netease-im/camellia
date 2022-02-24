package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.dashboard.daowrapper.ResourceInfoDaoWrapper;
import com.netease.nim.camellia.dashboard.daowrapper.TableDaoWrapper;
import com.netease.nim.camellia.dashboard.daowrapper.TableRefDaoWrapper;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import com.netease.nim.camellia.dashboard.model.Table;
import com.netease.nim.camellia.dashboard.model.TableRef;
import com.netease.nim.camellia.dashboard.model.ValidFlag;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.util.ResourceInfoTidsUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/5/17.
 */
@Service
public class TableService {

    @Autowired
    private TableDaoWrapper tableDao;

    @Autowired
    private TableRefDaoWrapper tableRefDao;

    @Autowired
    private ResourceInfoDaoWrapper resourceInfoDaoWrapper;

    @Autowired
    private ResourceCheckService resourceCheckService;

    public Table create(ResourceTable resourceTable, String info) {
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            LogBean.get().addProps("resourceTable.check", false);
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "resourceTable check fail");
        }

        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource resource : allResources) {
            if (!resourceCheckService.check(resource.getUrl())) {
                throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "resource.url=[" + resource.getUrl() + "] check fail");
            }
        }

        long now = System.currentTimeMillis();

        Table table = new Table();
        table.setDetail(ReadableResourceTableUtil.readableResourceTable(resourceTable));
        table.setInfo(info);
        table.setValidFlag(ValidFlag.VALID.getValue());

        table.setCreateTime(now);
        table.setUpdateTime(now);
        int create = tableDao.create(table);
        LogBean.get().addProps("create", create);

        for (Resource resource : allResources) {
            ResourceInfo resourceInfo = resourceInfoDaoWrapper.getByUrl(resource.getUrl());
            if (resourceInfo == null) {
                resourceInfo = new ResourceInfo();
                resourceInfo.setUrl(resource.getUrl());
                resourceInfo.setInfo("");
                Set<Long> tids = new HashSet<>();
                tids.add(table.getTid());
                resourceInfo.setTids(ResourceInfoTidsUtil.toString(tids));
                resourceInfo.setCreateTime(now);
                resourceInfo.setUpdateTime(now);
                int insert = resourceInfoDaoWrapper.save(resourceInfo);
                LogBean.get().addDebugProps("resource.url=[" + resource.getUrl() + "].insert", insert);
            } else {
                String tids = resourceInfo.getTids();
                Set<Long> set = ResourceInfoTidsUtil.parseTids(tids);
                set.add(table.getTid());
                resourceInfo.setTids(ResourceInfoTidsUtil.toString(set));
                resourceInfo.setUpdateTime(now);
                int update = resourceInfoDaoWrapper.save(resourceInfo);
                LogBean.get().addDebugProps("resource.url=[" + resource.getUrl() + "].update", update);
            }
        }
        return table;
    }

    public Table get(long tid) {
        return tableDao.get(tid);
    }

    public List<Table> getList(boolean onlyValid) {
        List<Table> list = tableDao.getList();
        if (onlyValid) {
            List<Table> validList = new ArrayList<>();
            for (Table table : list) {
                if (table.getValidFlag() == ValidFlag.VALID.getValue()) {
                    validList.add(table);
                }
            }
            return validList;
        } else {
            return list;
        }
    }

    public int delete(long tid) {
        Table table = tableDao.get(tid);
        if (table == null) {
            LogBean.get().addProps("tid.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not exists");
        }
        if (table.getValidFlag() == ValidFlag.NOT_VALID.getValue()) {
            LogBean.get().addProps("not.valid", true);
            return 1;
        }
        List<TableRef> refs = tableRefDao.getByTid(tid);
        if (refs != null) {
            for (TableRef tableRef : refs) {
                if (tableRef.getValidFlag() == ValidFlag.VALID.getValue()) {
                    Long bid = tableRef.getBid();
                    String bgroup = tableRef.getBgroup();
                    LogBean.get().addProps("bid.refs", bid);
                    LogBean.get().addProps("bgroup.refs", bgroup);
                    throw new AppException(CamelliaApiCode.FORBIDDEN.getCode(), "bid=" + bid + ",bgroup=" + bgroup + " refs");
                }
            }
        }
        int delete = tableDao.delete(table);
        LogBean.get().addProps("delete", delete);
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(table.getDetail());
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource resource : allResources) {
            ResourceInfo resourceInfo = resourceInfoDaoWrapper.getByUrl(resource.getUrl());
            if (resourceInfo == null) continue;
            Set<Long> set = ResourceInfoTidsUtil.parseTids(resourceInfo.getTids());
            set.remove(tid);
            resourceInfo.setTids(ResourceInfoTidsUtil.toString(set));
            int update = resourceInfoDaoWrapper.save(resourceInfo);
            LogBean.get().addDebugProps("resource.url=[" + resource.getUrl() + "].update", update);
        }
        return delete;
    }

}
