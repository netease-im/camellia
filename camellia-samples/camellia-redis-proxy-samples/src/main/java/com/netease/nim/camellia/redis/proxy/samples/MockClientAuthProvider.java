package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.command.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;

public class MockClientAuthProvider implements ClientAuthProvider {
    private CamelliaServerProperties properties;

    public MockClientAuthProvider(CamelliaServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientIdentity auth(String password) {
        ClientIdentity clientIdentity = new ClientIdentity();

        clientIdentity.setPass(true);
        clientIdentity.setBid(2L);
        clientIdentity.setBgroup("xxx");

        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}
