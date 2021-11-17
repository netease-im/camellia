package com.netease.nim.camellia.redis.proxy.mq.common;

import com.netease.nim.camellia.redis.proxy.command.Command;

public class MqPack {

    private Command command;
    private Long bid;
    private String bgroup;
    private String ext;

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
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

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
}
