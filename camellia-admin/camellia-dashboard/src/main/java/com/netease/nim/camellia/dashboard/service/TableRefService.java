package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.dashboard.daowrapper.TableDaoWrapper;
import com.netease.nim.camellia.dashboard.daowrapper.TableRefDaoWrapper;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.*;
import com.netease.nim.camellia.dashboard.util.LogBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Service
public class TableRefService {

    @Autowired
    private TableRefDaoWrapper tableRefDao;

    @Autowired
    private TableDaoWrapper tableDao;

    public TableWithTableRef createOrUpdate(long bid, String bgroup, long tid, String info) {
        Table table = tableDao.get(tid);
        if (table == null) {
            LogBean.get().addProps("tid.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not exists");
        }
        if (table.getValidFlag() == ValidFlag.NOT_VALID.getValue()) {
            LogBean.get().addProps("tid.valid", false);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not valid");
        }
        TableRef tableRef = tableRefDao.getByBidBGroup(bid, bgroup);
        long now = System.currentTimeMillis();
        if (tableRef == null) {
            tableRef = new TableRef();
            tableRef.setBid(bid);
            tableRef.setBgroup(bgroup);
            tableRef.setTid(tid);
            tableRef.setInfo(info);
            tableRef.setValidFlag(ValidFlag.VALID.getValue());
            tableRef.setCreateTime(now);
            tableRef.setUpdateTime(now);
            int create = tableRefDao.save(tableRef);
            LogBean.get().addProps("create", create);
        } else {
            tableRef.setTid(tid);
            if (info != null) {
                tableRef.setInfo(info);
            }
            tableRef.setValidFlag(ValidFlag.VALID.getValue());
            tableRef.setUpdateTime(now);
            int update = tableRefDao.save(tableRef);
            LogBean.get().addProps("update", update);
        }
        TableWithTableRef tableWithTableRef=new TableWithTableRef();
        tableWithTableRef.setTable(table);
        tableWithTableRef.setTableRef(tableRef);
        return tableWithTableRef;
    }

    public TableRef getByBidBGroup(long bid, String bgroup) {
        return tableRefDao.getByBidBGroup(bid, bgroup);
    }

    public List<TableRef> getByTid(long tid, boolean onlyValid) {
        List<TableRef> list = tableRefDao.getByTid(tid);
        if (onlyValid) {
            return filterValid(list);
        } else {
            return list;
        }
    }

    public List<TableRef> getByBid(long bid, boolean onlyValid) {
        List<TableRef> list = tableRefDao.getByBid(bid);
        if (onlyValid) {
            return filterValid(list);
        } else {
            return list;
        }
    }

    public List<TableRef> getList(boolean onlyValid) {
        List<TableRef> list = tableRefDao.getList();
        if (onlyValid) {
            return filterValid(list);
        } else {
            return list;
        }
    }

    public TableRefPage getRefsList(Long tid, Long bid, String bgroup, Integer validFlag,String info,String resourceInfo,Integer pageNum,Integer pageSize){
        TableRefPage tableRefPage=new TableRefPage();
        Integer currentNum = (pageNum - 1) * pageSize;
        List<TableRefAddition> byTidBidBgroupValidFlag = tableRefDao.getByTidBidBgroupValidFlag(tid, bid, bgroup, validFlag,info,resourceInfo,currentNum,pageSize);
        tableRefPage.setTableRefs(byTidBidBgroupValidFlag);
        Integer count=tableRefDao.countByTidBidBgroupValidFlag(tid, bid, bgroup, validFlag,info,resourceInfo);
        tableRefPage.setCount(count);
        return tableRefPage;
    }

    public int delete(long bid, String bgroup) {
        TableRef tableRef = tableRefDao.getByBidBGroup(bid, bgroup);
        if (tableRef == null) {
            LogBean.get().addProps("bid/group.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "bid/group not exists");
        }
        if (tableRef.getValidFlag() == ValidFlag.NOT_VALID.getValue()) {
            LogBean.get().addProps("bid/group.valid", false);
            return 1;
        }
        tableRef.setValidFlag(ValidFlag.NOT_VALID.getValue());
        tableRef.setUpdateTime(System.currentTimeMillis());
        int update = tableRefDao.save(tableRef);
        LogBean.get().addProps("update", update);
        return update;
    }

    private List<TableRef> filterValid(List<TableRef> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<TableRef> validList = new ArrayList<>();
        for (TableRef tableRef : list) {
            if (tableRef.getValidFlag() == ValidFlag.VALID.getValue()) {
                validList.add(tableRef);
            }
        }
        return validList;
    }
}
