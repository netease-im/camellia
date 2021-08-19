package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.command.auth.ClientIdentity;

public class MockClientAuthProvider implements ClientAuthProvider {

    @Override
    public ClientIdentity auth(String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (password.equals("pass1")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(1L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass2")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(2L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass3")) {
            clientIdentity.setPass(true);
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}
