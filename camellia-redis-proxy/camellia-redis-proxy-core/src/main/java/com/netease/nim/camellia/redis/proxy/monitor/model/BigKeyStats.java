package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class BigKeyStats {
    private String bid;
    private String bgroup;
    private String commandType;
    private String command;
    private String key;
    private long size;
    private long threshold;

    /**
     * default constructor
     */
    public BigKeyStats() {
    }

    /**
     * get bid
     * @return bid
     */
    public String getBid() {
        return bid;
    }

    /**
     * set bid
     * @param bid bid
     */
    public void setBid(String bid) {
        this.bid = bid;
    }

    /**
     * get bgroup
     * @return bgroup
     */
    public String getBgroup() {
        return bgroup;
    }

    /**
     * set bgroup
     * @param bgroup bgroup
     */
    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    /**
     * get command type
     * @return type
     */
    public String getCommandType() {
        return commandType;
    }

    /**
     * set command type
     * @param commandType type
     */
    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    /**
     * get command
     * @return command
     */
    public String getCommand() {
        return command;
    }

    /**
     * set command
     * @param command command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * get key
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * set key
     * @param key key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * get size
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * set size
     * @param size size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * get threshold
     * @return threshold
     */
    public long getThreshold() {
        return threshold;
    }

    /**
     * set threshold
     * @param threshold threshold
     */
    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }
}
