package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.dashboard.daowrapper.ResourceInfoDaoWrapper;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.util.ResourceInfoTidsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/11/21.
 */
@Service
public class ResourceInfoService {

    @Autowired
    private ResourceInfoDaoWrapper resourceInfoDaoWrapper;

    @Autowired
    private ResourceCheckService resourceCheckService;

    public int createOrUpdateResource(String url, String info) {
        boolean check = resourceCheckService.check(url);
        if (!check) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "url check fail");
        }

        ResourceInfo resourceInfo = resourceInfoDaoWrapper.getByUrl(url);
        long now = System.currentTimeMillis();
        if (resourceInfo == null) {
            resourceInfo = new ResourceInfo();
            resourceInfo.setUrl(url);
            resourceInfo.setTids("");
            resourceInfo.setInfo(info);
            resourceInfo.setCreateTime(now);
            resourceInfo.setUpdateTime(now);
            int insert = resourceInfoDaoWrapper.save(resourceInfo);
            LogBean.get().addProps("insert", insert);
            return insert;
        } else {
            resourceInfo.setInfo(info);
            resourceInfo.setUpdateTime(now);
            int update = resourceInfoDaoWrapper.save(resourceInfo);
            LogBean.get().addProps("update", update);
            return update;
        }
    }

    public ResourceInfo get(long id) {
        return resourceInfoDaoWrapper.get(id);
    }

    public int delete(long id) {
        ResourceInfo resourceInfo = resourceInfoDaoWrapper.get(id);
        if (resourceInfo == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "resource not exists");
        }
        Set<Long> tids = ResourceInfoTidsUtil.parseTids(resourceInfo.getTids());
        if (!tids.isEmpty()) {
            throw new AppException(CamelliaApiCode.FORBIDDEN.getCode(), "resource has refs");
        }
        return resourceInfoDaoWrapper.delete(id);
    }

    public ResourceInfo getByUrl(String url) {
        return resourceInfoDaoWrapper.getByUrl(url);
    }

    public List<ResourceInfo> getList() {
        return resourceInfoDaoWrapper.getList();
    }
}
