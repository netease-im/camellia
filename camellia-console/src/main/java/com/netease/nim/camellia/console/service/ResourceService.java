package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.service.bo.ResourceInfoBO;
import com.netease.nim.camellia.console.service.vo.CamelliaResourcePage;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface ResourceService {

    void createOrUpdateResource(Long dashboardId, String url, String info);

    void delete(Long dashboardId, Long id);

    CamelliaResourcePage getAllUrl(Long did, String url, int pageNum, int pageSize);

    List<ResourceInfoBO> getResourceQuery(Long did, String url, int size);
}
