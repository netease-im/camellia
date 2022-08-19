package com.netease.nim.camellia.console.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.constant.DashboardPathConstant;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.dao.wrapper.CamelliaRefHistoryMapperWrapper;
import com.netease.nim.camellia.console.dao.wrapper.CamelliaTableHistoryMapperWrapper;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import com.netease.nim.camellia.console.model.TableDetail;
import com.netease.nim.camellia.console.service.TableService;
import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.bo.TableRefBO;
import com.netease.nim.camellia.console.service.vo.CamelliaTablePage;
import com.netease.nim.camellia.console.service.vo.TableWithTableRefs;
import com.netease.nim.camellia.console.util.*;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.nim.camellia.console.constant.ModelConstant.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class TableServiceImpl implements TableService {
    private static final Logger logger = LoggerFactory.getLogger(TableServiceImpl.class);


    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired
    CamelliaTableHistoryMapperWrapper tableHistoryMapperWrapper;

    @Autowired
    CamelliaRefHistoryMapperWrapper refHistoryMapperWrapper;




    @Override
    public List<TableBO> getAllTable(Long dashboardId, boolean onlyValid) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);

        String address = byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        paras.put("onlyValid", onlyValid + "");
        RespBody body = OkhttpKit.getRequest(address + DashboardPathConstant.resourceTableList, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        JSONArray data = jsonObject.getJSONArray("data");
        return data.toJavaList(TableBO.class);
    }

    @Override
    public TableBO getTable(Long dashboardId, long tid) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);

        String address = byId.getAddress();
        RespBody body = OkhttpKit.getRequest(address + String.format(DashboardPathConstant.resourceTableTid, tid), null, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);

        return jsonObject.getObject("data", TableBO.class);
    }

    @Override
    public TableBO createTable(Long dashboardId, String table, String info,Integer type) {
        String detail=null;
        if(type==0){
            detail=TableCheckUtil.parseConsoleTableToDashboard(JSONObject.parseObject(table,TableDetail.class));
        }
        else {
            detail=table;
        }
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);

        String address = byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        paras.put("detail", detail);
        paras.put("info", info);
        RespBody respBody = OkhttpKit.postForm(address + DashboardPathConstant.createResourceTablePath, paras, null);
        //失败会抛出异常
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(respBody);
        TableBO data = jsonObject.getObject("data", TableBO.class);
        CamelliaTableHistory tableHistory = generateTableHistory(byId);
        tableHistory.setOpType(ADD);
        tableHistory.setDetail(data.getTable());
        tableHistory.setResourceInfo(info);
        tableHistory.setTid(data.getTid());
        tableHistoryMapperWrapper.saveCamelliaTableHistory(tableHistory);
        return data;
    }

    @Override
    public void deleteTable(long dashboardId, long tid) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();
        RespBody respBody = OkhttpKit.deleteRequest(address + String.format(DashboardPathConstant.deleteResourceTable, tid), null, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(respBody);
        CamelliaTableHistory tableHistory = generateTableHistory(byId);
        tableHistory.setTid(tid);
        tableHistory.setOpType(DELETE);
        tableHistoryMapperWrapper.saveCamelliaTableHistory(tableHistory);
    }

    @Override
    @Transactional
    public TableWithTableRefs changeTable(Long dashboardId, Long tid, String table, String info,Integer type) {
        String detail=null;
        if(type==0){
            detail=TableCheckUtil.parseConsoleTableToDashboard(JSONObject.parseObject(table,TableDetail.class));
        }
        else {
            detail=table;
        }

        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();


        Map<String, String> paras = new HashMap<>();
        paras.put("tid", tid + "");
        paras.put("detail", detail);
        paras.put("info", info);
        RespBody respBody = OkhttpKit.postForm(address + DashboardPathConstant.changeResourceTable, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(respBody);
        long now = System.currentTimeMillis();


        TableWithTableRefs data = jsonObject.getObject("data", TableWithTableRefs.class);
        TableBO tableBO = data.getTable();
        CamelliaTableHistory tableHistory = generateTableHistory(byId);
        tableHistory.setOpType(MODIFY);
        tableHistory.setDetail(tableBO.getTable());
        tableHistory.setResourceInfo(info);
        tableHistory.setTid(tableBO.getTid());
        tableHistoryMapperWrapper.saveCamelliaTableHistory(tableHistory);

        List<TableRefBO> tableRefBOs = data.getTableRefs();
        int refsNum = 0;
        if (tableRefBOs != null && !tableRefBOs.isEmpty()) {
            List<CamelliaRefHistory> camelliaRefHistories = new ArrayList<>();
            for (TableRefBO tableRefBO : tableRefBOs) {
                CamelliaRefHistory camelliaRefHistory = new CamelliaRefHistory();
                camelliaRefHistory.setAddress(byId.getAddress());
                camelliaRefHistory.setDashboardId(byId.getDid());
                camelliaRefHistory.setUsername(AppInfoContext.getUser().getUsername());
                camelliaRefHistory.setBid(tableRefBO.getBid());
                camelliaRefHistory.setBgroup(tableRefBO.getBgroup());
                camelliaRefHistory.setRefInfo(tableRefBO.getInfo());
                camelliaRefHistory.setTid(tableBO.getTid());
                camelliaRefHistory.setResourceInfo(info);
                camelliaRefHistory.setDetail(tableBO.getTable());
                camelliaRefHistory.setCreatedTime(now);
                camelliaRefHistory.setOpType(BIND);
                camelliaRefHistories.add(camelliaRefHistory);

            }
            refsNum = refHistoryMapperWrapper.saveCamelliaRefHistories(camelliaRefHistories);
        }
        LogBean.get().addProps("affect refs num", refsNum);
        TableWithTableRefs ret = new TableWithTableRefs();
        ret.setDashboard(byId);
        ret.setTable(tableBO);
        ret.setTableRefs(tableRefBOs);
        return ret;
    }

    @Override
    public CamelliaTablePage getAllTableTidInfo(Long did, Integer validFlag, Long tid, String info, String table, int pageNum, int pageSize) {
        CamelliaDashboard byId = getCamelliaDashboardById(did);
        String address = byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        if (tid != null) {
            paras.put("tid", tid + "");
        }
        if (!StringUtil.isNullOrEmpty(info)) {
            paras.put("info", info);
        }
        if (validFlag != null) {
            paras.put("validFlag", validFlag + "");
        }
        if(table!=null){
            paras.put("detail",table);
        }
        paras.put("pageSize", pageSize + "");
        paras.put("pageNum", pageNum + "");
        RespBody body = OkhttpKit.getRequest(address + DashboardPathConstant.resourceTableAll, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        CamelliaTablePage data = jsonObject.getObject("data", CamelliaTablePage.class);
//        List<TableBO> tables = data.getTables();
//        if (tables != null && !tables.isEmpty()) {
//            for (TableBO tableBO : tables) {
//                TableDetail tableDetail = TableCheckUtil.parseDashboardTableToConsole(tableBO.getTable());
//                tableBO.setTable(JSON.toJSONString(tableDetail));
//            }
//        }
        return data;
    }

    private CamelliaTableHistory generateTableHistory(CamelliaDashboard byId) {
        CamelliaTableHistory tableHistory = new CamelliaTableHistory();
        tableHistory.setAddress(byId.getAddress());
        tableHistory.setDashboardId(byId.getDid());
        tableHistory.setUsername(AppInfoContext.getUser().getUsername());
        tableHistory.setCreatedTime(System.currentTimeMillis());
        return tableHistory;
    }

    private CamelliaDashboard getCamelliaDashboardById(Long id) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null || byId.getIsUse() == notUse) {
            throw new AppException(AppCode.DASHBOARD_CAN_NOT_USE, id + " unavailable");
        }
        return byId;
    }
}
