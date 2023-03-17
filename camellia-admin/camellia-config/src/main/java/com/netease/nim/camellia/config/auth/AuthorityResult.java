package com.netease.nim.camellia.config.auth;

/**
 * Created by caojiajun on 2023/3/15
 */
public class AuthorityResult {
    private boolean pass;
    private UserInfo userInfo;

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
