package com.netease.nim.camellia.dashboard.model;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class ResourceInfoPage {
    private List<ResourceInfo> resourceInfos;
    private Integer count;

    public List<ResourceInfo> getResourceInfos() {
        return resourceInfos;
    }

    public void setResourceInfos(List<ResourceInfo> resourceInfos) {
        this.resourceInfos = resourceInfos;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
