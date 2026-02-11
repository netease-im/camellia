package com.netease.nim.camellia.redis.proxy.auth;

public class ClientIdentity {

    public static final ClientIdentity AUTH_FAIL = new ClientIdentity(-1L, "default", false);

    private Long bid;
    private String bgroup;

    private boolean pass;

    public ClientIdentity(Long bid, String bgroup, boolean pass) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.pass = pass;
    }

    public ClientIdentity() {
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }
}
