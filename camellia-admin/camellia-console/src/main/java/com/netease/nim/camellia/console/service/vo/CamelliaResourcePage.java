package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.service.bo.ResourceInfoBO;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaResourcePage {
    private List<ResourceInfoBO> resourceInfos;
    private Integer count;

    public List<ResourceInfoBO> getResourceInfos() {
        return resourceInfos;
    }

    public void setResourceInfos(List<ResourceInfoBO> resourceInfos) {
        this.resourceInfos = resourceInfos;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
