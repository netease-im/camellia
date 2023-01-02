package com.netease.nim.camellia.console.springboot;

import com.netease.nim.camellia.console.conf.PropertyConstant;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@ConfigurationProperties(prefix = "camellia-console")
public class CamelliaConsoleConfiguration {
    private int heartCallSeconds = PropertyConstant.heartCallSeconds;
    private int reloadSeconds = PropertyConstant.reloadSeconds;

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
