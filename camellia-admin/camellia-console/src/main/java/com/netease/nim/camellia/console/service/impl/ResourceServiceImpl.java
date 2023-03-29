package com.netease.nim.camellia.console.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.constant.DashboardPathConstant;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.service.OperationNotify;
import com.netease.nim.camellia.console.service.ResourceService;
import com.netease.nim.camellia.console.service.bo.ResourceInfoBO;
import com.netease.nim.camellia.console.service.vo.CamelliaResourcePage;
import com.netease.nim.camellia.console.util.OkhttpKit;
import com.netease.nim.camellia.console.util.RespBody;
import com.netease.nim.camellia.console.util.ResponseCheckUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.nim.camellia.console.constant.ModelConstant.notUse;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class ResourceServiceImpl implements ResourceService {
    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired(required = false)
    OperationNotify notify;

    @Override
    public CamelliaResourcePage getAllUrl(Long did, String url, int pageNum, int pageSize) {
        CamelliaDashboard byId = getCamelliaDashboardById(did);
        String address = byId.getAddress();
        Map<String,String> paras=new HashMap<>();
        paras.put("url",url);
        paras.put("pageSize",pageSize+"");
        paras.put("pageNum",pageNum+"");
        RespBody body = OkhttpKit.getRequest(address + DashboardPathConstant.getResources, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        return jsonObject.getObject("data", CamelliaResourcePage.class);

    }

    @Override
    public List<ResourceInfoBO> getResourceQuery(Long did, String url, int size) {
        CamelliaDashboard byId = getCamelliaDashboardById(did);
        String address = byId.getAddress();
        Map<String,String> paras=new HashMap<>();
        paras.put("url",url);
        paras.put("size",size+"");
        RespBody body = OkhttpKit.getRequest(address + DashboardPathConstant.getResourceQuery, paras, null);
        JSONObject jsonObject = ResponseCheckUtil.parseResponse(body);
        return  jsonObject.getJSONArray("data").toJavaList(ResourceInfoBO.class);
    }


    @Override
    public void createOrUpdateResource(Long dashboardId, String url, String info) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();
        Map<String,String> paras=new HashMap<>();
        paras.put("url",url);
        paras.put("info",info);
        RespBody body = OkhttpKit.postForm(address + DashboardPathConstant.createOrUpdateResources, paras, null);
        ResponseCheckUtil.parseResponse(body);

        if (notify != null) {
            notify.notifyCreateOrUpdateResource(AppInfoContext.getUser().getUsername(), address, url, info);
        }

    }

    @Override
    public void delete(Long dashboardId, Long id) {
        CamelliaDashboard byId = getCamelliaDashboardById(dashboardId);
        String address = byId.getAddress();
        Map<String,String> paras=new HashMap<>();
        paras.put("id",id+"");
        RespBody body = OkhttpKit.deleteRequest(address + DashboardPathConstant.deleteResource, paras, null,null);
        ResponseCheckUtil.parseResponse(body);

        if (notify != null) {
            notify.notifyDeleteResource(AppInfoContext.getUser().getUsername(), address, id);
        }
    }




    private CamelliaDashboard getCamelliaDashboardById(Long id){
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null || byId.getIsUse() == notUse) {
            throw new AppException(AppCode.DASHBOARD_CAN_NOT_USE, id + " unavailable");
        }
        return byId;
    }
}
