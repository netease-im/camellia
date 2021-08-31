package com.netease.nim.camellia.redis.proxy.command.auth;

public interface ClientAuthProvider {

    /**
     * 验证密码
     * @param userName 账号，可能为null
     * @param password 密码
     * @return ClientIdentity
     */
    ClientIdentity auth(String userName, String password);

    /**
     * Provider是否设置了密码
     * @return true/false
     */
    boolean isPasswordRequired();
}
