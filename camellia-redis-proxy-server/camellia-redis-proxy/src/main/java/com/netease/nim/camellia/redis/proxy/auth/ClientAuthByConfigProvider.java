package com.netease.nim.camellia.redis.proxy.auth;

import io.netty.util.internal.StringUtil;

public class ClientAuthByConfigProvider implements ClientAuthProvider {
    private final String configPassword;

    public ClientAuthByConfigProvider(String password) {
        this.configPassword = password;
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (userName != null && !userName.equals("default")) {
            clientIdentity.setPass(false);
            return clientIdentity;
        }
        clientIdentity.setPass(!StringUtil.isNullOrEmpty(this.configPassword)
                && this.configPassword.equals(password));
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return !StringUtil.isNullOrEmpty(this.configPassword);
    }
}
