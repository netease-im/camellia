package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2024/9/12
 */
public class LocalStorageRunToCompletionStats {

    private String command;
    private long hit;
    private long notHit;
    private double hitRate;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getHit() {
        return hit;
    }

    public void setHit(long hit) {
        this.hit = hit;
    }

    public long getNotHit() {
        return notHit;
    }

    public void setNotHit(long notHit) {
        this.notHit = notHit;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }
}
