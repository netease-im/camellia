package com.netease.nim.camellia.hot.key.server.springboot.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
@ConfigurationProperties(prefix = "camellia-hot-key-server.netty")
public class NettyProperties {
    private int bossThread = HotKeyConstants.Server.nettyBossThread;
    private int workThread = -1;
    private boolean tcpNoDelay = HotKeyConstants.Server.tcpNoDelay;
    private int soBacklog = HotKeyConstants.Server.soBacklog;
    private int soSndbuf = HotKeyConstants.Server.soSndbuf;
    private int soRcvbuf = HotKeyConstants.Server.soRcvbuf;
    private boolean soKeepalive = HotKeyConstants.Server.soKeepalive;
    private int writeBufferWaterMarkLow = HotKeyConstants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = HotKeyConstants.Server.writeBufferWaterMarkHigh;

    public int getBossThread() {
        return bossThread;
    }

    public void setBossThread(int bossThread) {
        this.bossThread = bossThread;
    }

    public int getWorkThread() {
        return workThread;
    }

    public void setWorkThread(int workThread) {
        this.workThread = workThread;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getSoBacklog() {
        return soBacklog;
    }

    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
    }

    public int getSoSndbuf() {
        return soSndbuf;
    }

    public void setSoSndbuf(int soSndbuf) {
        this.soSndbuf = soSndbuf;
    }

    public int getSoRcvbuf() {
        return soRcvbuf;
    }

    public void setSoRcvbuf(int soRcvbuf) {
        this.soRcvbuf = soRcvbuf;
    }

    public boolean isSoKeepalive() {
        return soKeepalive;
    }

    public void setSoKeepalive(boolean soKeepalive) {
        this.soKeepalive = soKeepalive;
    }

    public int getWriteBufferWaterMarkLow() {
        return writeBufferWaterMarkLow;
    }

    public void setWriteBufferWaterMarkLow(int writeBufferWaterMarkLow) {
        this.writeBufferWaterMarkLow = writeBufferWaterMarkLow;
    }

    public int getWriteBufferWaterMarkHigh() {
        return writeBufferWaterMarkHigh;
    }

    public void setWriteBufferWaterMarkHigh(int writeBufferWaterMarkHigh) {
        this.writeBufferWaterMarkHigh = writeBufferWaterMarkHigh;
    }
}
