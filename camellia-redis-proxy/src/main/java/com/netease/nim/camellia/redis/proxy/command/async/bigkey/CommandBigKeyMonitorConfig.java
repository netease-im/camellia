package com.netease.nim.camellia.redis.proxy.command.async.bigkey;


/**
 *
 * Created by caojiajun on 2020/11/10
 */
public class CommandBigKeyMonitorConfig {

    private final int stringSizeThreshold;
    private final int listSizeThreshold;
    private final int zsetSizeThreshold;
    private final int hashSizeThreshold;
    private final int setSizeThreshold;

    private final BigKeyMonitorCallback bigKeyMonitorCallback;

    public CommandBigKeyMonitorConfig(int stringSizeThreshold,
                                      int listSizeThreshold, int zsetSizeThreshold,
                                      int hashSizeThreshold, int setSizeThreshold,
                                      BigKeyMonitorCallback bigKeyMonitorCallback) {
        this.stringSizeThreshold = stringSizeThreshold;
        this.listSizeThreshold = listSizeThreshold;
        this.zsetSizeThreshold = zsetSizeThreshold;
        this.hashSizeThreshold = hashSizeThreshold;
        this.setSizeThreshold = setSizeThreshold;
        this.bigKeyMonitorCallback = bigKeyMonitorCallback;
    }

    public int getStringSizeThreshold() {
        return stringSizeThreshold;
    }

    public int getListSizeThreshold() {
        return listSizeThreshold;
    }

    public int getZsetSizeThreshold() {
        return zsetSizeThreshold;
    }

    public int getHashSizeThreshold() {
        return hashSizeThreshold;
    }

    public int getSetSizeThreshold() {
        return setSizeThreshold;
    }

    public BigKeyMonitorCallback getBigKeyMonitorCallback() {
        return bigKeyMonitorCallback;
    }
}
