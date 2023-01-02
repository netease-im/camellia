package com.netease.nim.camellia.console.context;

import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class AppInfoContext {
    private static ThreadLocal<AppInfoContext> appLoginBean = new ThreadLocal<>();

    private BaseUser user;

    private IdentityDashboardBaseAO baseAO;

    public static void before() {
        setUser(null);
        setIdentityDashboardBaseAO(null);
    }

    public static BaseUser getUser() {
        return get().user;
    }

    public static void setUser(BaseUser user) {
        get().user = user;
    }

    public static IdentityDashboardBaseAO getIdentityDashboardBaseAO() {
        return get().baseAO;
    }

    public static void setIdentityDashboardBaseAO(IdentityDashboardBaseAO baseAO) {
        get().baseAO = baseAO;
    }

    public static AppInfoContext get() {
        AppInfoContext bean = appLoginBean.get();
        if (bean == null) {
            bean = new AppInfoContext();
            appLoginBean.set(bean);
        }
        return bean;
    }
}
