package com.netease.nim.camellia.console.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.constant.DashboardPathConstant;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.dao.wrapper.CamelliaRefHistoryMapperWrapper;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import com.netease.nim.camellia.console.service.OperationNotify;
import com.netease.nim.camellia.console.service.TableRefService;
import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.bo.TableRefBO;
import com.netease.nim.camellia.console.service.vo.CamelliaTableRefPage;
import com.netease.nim.camellia.console.util.OkhttpKit;
import com.netease.nim.camellia.console.util.RespBody;
import com.netease.nim.camellia.console.util.ResponseCheckUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.netease.nim.camellia.console.constant.ModelConstant.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class TableRefServiceImpl implements TableRefService {
    private static final Logger logger = LoggerFactory.getLogger(TableRefServiceImpl.class);

    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired
    CamelliaRefHistoryMapperWrapper refHistoryMapperWrapper;

    @Autowired(required = false)
    private OperationNotify notify;

    @Override
    public TableRefBO bindBigBgroupWithTid(Long dashboardId, long bid, String bgroup, long tid, String info) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        paras.put("bid",bid+"");
        paras.put("bgroup",bgroup);
        paras.put("tid",tid+"");
        paras.put("info",info);
        RespBody body = OkhttpKit.postForm(address +DashboardPathConstant.createOrUpdateTableRef, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        JSONObject data = jsonObject.getJSONObject("data");
        TableBO table=data.getObject("table",TableBO.class);
        TableRefBO tableRefBO=data.getObject("tableRef",TableRefBO.class);
        tableRefBO.setResourceInfo(table.getInfo());
        CamelliaRefHistory camelliaRefHistory=generateRefHistory(byId,table,tableRefBO);
        camelliaRefHistory.setOpType(BIND);
        refHistoryMapperWrapper.saveCamelliaRefHistory(camelliaRefHistory);
        if (notify != null) {
            notify.notifyBindBigBgroupWithTid(AppInfoContext.getUser().getUsername(), address, bid, bgroup, tid, info);
        }
        return tableRefBO;
    }

    @Override
    public void delete(Long dashboardId, long bid, String bgroup) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        paras.put("bid",bid+"");
        paras.put("bgroup",bgroup);
        RespBody body = OkhttpKit.deleteRequest(address +DashboardPathConstant.deleteTableRefs,paras, null,null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        CamelliaRefHistory camelliaRefHistory=new CamelliaRefHistory();
        camelliaRefHistory.setAddress(byId.getAddress());
        camelliaRefHistory.setDashboardId(byId.getDid());
        camelliaRefHistory.setUsername(AppInfoContext.getUser().getUsername());
        camelliaRefHistory.setBid(bid);
        camelliaRefHistory.setBgroup(bgroup);
        camelliaRefHistory.setOpType(CLEAR);
        camelliaRefHistory.setCreatedTime(System.currentTimeMillis());
        refHistoryMapperWrapper.saveCamelliaRefHistory(camelliaRefHistory);
        if (notify != null) {
            notify.notifyDeleteTableRef(AppInfoContext.getUser().getUsername(), address, bid, bgroup);
        }
    }

    @Override
    public CamelliaTableRefPage getTableRef(Long did, Long bid, String bgroup, Long tid, Integer validFlag, int pageNum, int pageSize,String info,String resourceInfo) {
        CamelliaDashboard byId = getCamelliaDashboardById(did);
        String address=byId.getAddress();
        Map<String, String> paras = new HashMap<>();
        if(bid!=null) {
            paras.put("bid", bid + "");
        }
        if(!StringUtil.isNullOrEmpty(bgroup)){

            paras.put("bgroup",bgroup);
        }
        if(tid!=null) {
            paras.put("tid", tid + "");
        }
        if(validFlag!=null){
            paras.put("validFlag",validFlag+"");
        }
        if(!StringUtil.isNullOrEmpty(info)){
            paras.put("info",info);
        }
        if(!StringUtil.isNullOrEmpty(resourceInfo)){
            paras.put("resourceInfo",resourceInfo);
        }
        paras.put("pageSize",pageSize+"");
        paras.put("pageNum",pageNum+"");
        RespBody body = OkhttpKit.getRequest(address +DashboardPathConstant.getTableRefAll, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        return jsonObject.getObject("data", CamelliaTableRefPage.class);
    }

    private CamelliaRefHistory generateRefHistory(CamelliaDashboard byId,TableBO table, TableRefBO tableRefBO) {
        CamelliaRefHistory camelliaRefHistory=new CamelliaRefHistory();
        camelliaRefHistory.setAddress(byId.getAddress());
        camelliaRefHistory.setDashboardId(byId.getDid());
        camelliaRefHistory.setUsername(AppInfoContext.getUser().getUsername());
        camelliaRefHistory.setBid(tableRefBO.getBid());
        camelliaRefHistory.setBgroup(tableRefBO.getBgroup());
        camelliaRefHistory.setRefInfo(tableRefBO.getInfo());
        camelliaRefHistory.setTid(table.getTid());
        camelliaRefHistory.setResourceInfo(table.getInfo());
        camelliaRefHistory.setDetail(table.getTable());
        camelliaRefHistory.setCreatedTime(System.currentTimeMillis());
        return camelliaRefHistory;
    }

    private CamelliaDashboard getCamelliaDashboardById(Long id){
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null || byId.getIsUse() == notUse) {
            throw new AppException(AppCode.DASHBOARD_CAN_NOT_USE, id + " unavailable");
        }
        return byId;
    }

}
