package com.netease.nim.camellia.dashboard.daowrapper;

import com.netease.nim.camellia.dashboard.dao.ResourceInfoDao;
import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 *
 * Created by caojiajun on 2019/11/20.
 */
@Service
public class ResourceInfoDaoWrapper {

    @Autowired
    private ResourceInfoDao resourceInfoDao;

    public ResourceInfo get(long id) {
        Optional<ResourceInfo> optional = resourceInfoDao.findById(id);
        return optional.orElse(null);
    }

    public int delete(long id) {
        resourceInfoDao.deleteById(id);
        return 0;
    }

    public ResourceInfo getByUrl(String url) {
        return resourceInfoDao.findByUrl(url);
    }

    public List<ResourceInfo> getList() {
        return resourceInfoDao.findAll();
    }

    public int save(ResourceInfo resourceInfo) {
        ResourceInfo save = resourceInfoDao.save(resourceInfo);
        resourceInfo.setId(save.getId());
        return 1;
    }

    public List<ResourceInfo> getPageList(int currentNum, Integer pageSize) {
        return resourceInfoDao.getPageList(currentNum,pageSize);
    }

    public int queryCount() {
        return resourceInfoDao.countAll();
    }
}
