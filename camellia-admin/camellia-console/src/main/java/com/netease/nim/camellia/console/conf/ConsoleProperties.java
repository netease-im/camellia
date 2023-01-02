package com.netease.nim.camellia.console.conf;


/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class ConsoleProperties {
    private int heartCallSeconds = 5;

    private int reloadSeconds=10;

    public int getReloadSeconds() {
        return reloadSeconds;
    }

    public void setReloadSeconds(int reloadSeconds) {
        this.reloadSeconds = reloadSeconds;
    }

    public int getHeartCallSeconds() {
        return heartCallSeconds;
    }

    public void setHeartCallSeconds(int heartCallSeconds) {
        this.heartCallSeconds = heartCallSeconds;
    }
}
