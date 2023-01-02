package com.netease.nim.camellia.console.service.ao;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class URLAO extends IdentityDashboardBaseAO {
    private String url;
    private String info;
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
