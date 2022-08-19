package com.netease.nim.camellia.console.service.vo;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class UserLoginVO {
    private String accessToken;
    private String username;
    private Integer TokenTtl;
    private Boolean loginSuccess;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getLoginSuccess() {
        return loginSuccess;
    }

    public void setLoginSuccess(Boolean loginSuccess) {
        this.loginSuccess = loginSuccess;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public Integer getTokenTtl() {
        return TokenTtl;
    }

    public void setTokenTtl(Integer tokenTtl) {
        TokenTtl = tokenTtl;
    }
}
