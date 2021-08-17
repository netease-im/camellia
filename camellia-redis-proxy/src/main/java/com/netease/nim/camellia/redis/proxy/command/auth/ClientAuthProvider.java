package com.netease.nim.camellia.redis.proxy.command.auth;

public interface ClientAuthProvider {

    /**
     * 验证密码
     *
     * @param password
     * @return
     */
    ClientIdentity auth(String password);

    /**
     * Provider是否设置了密码
     *
     * @return
     */
    boolean isPasswordRequired();
}
