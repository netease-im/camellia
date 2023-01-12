package com.netease.nim.camellia.redis.proxy.springboot.conf;

import com.netease.nim.camellia.redis.proxy.conf.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
@ConfigurationProperties(prefix = "camellia-redis-proxy.netty")
public class NettyProperties {
    private int bossThread = 1;
    private int workThread = -1;
    private boolean tcpNoDelay = Constants.Server.tcpNoDelay;
    private int soBacklog = Constants.Server.soBacklog;
    private int soSndbuf = Constants.Server.soSndbuf;
    private int soRcvbuf = Constants.Server.soRcvbuf;
    private boolean soKeepalive = Constants.Server.soKeepalive;
    private int readerIdleTimeSeconds = Constants.Server.readerIdleTimeSeconds;
    private int writerIdleTimeSeconds = Constants.Server.writerIdleTimeSeconds;
    private int allIdleTimeSeconds = Constants.Server.allIdleTimeSeconds;
    private int writeBufferWaterMarkLow = Constants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = Constants.Server.writeBufferWaterMarkHigh;
    private int commandDecodeMaxBatchSize = Constants.Server.commandDecodeMaxBatchSize;
    private int commandDecodeBufferInitializerSize = Constants.Server.commandDecodeBufferInitializerSize;

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

    public int getReaderIdleTimeSeconds() {
        return readerIdleTimeSeconds;
    }

    public void setReaderIdleTimeSeconds(int readerIdleTimeSeconds) {
        this.readerIdleTimeSeconds = readerIdleTimeSeconds;
    }

    public int getWriterIdleTimeSeconds() {
        return writerIdleTimeSeconds;
    }

    public void setWriterIdleTimeSeconds(int writerIdleTimeSeconds) {
        this.writerIdleTimeSeconds = writerIdleTimeSeconds;
    }

    public int getAllIdleTimeSeconds() {
        return allIdleTimeSeconds;
    }

    public void setAllIdleTimeSeconds(int allIdleTimeSeconds) {
        this.allIdleTimeSeconds = allIdleTimeSeconds;
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

    public int getCommandDecodeMaxBatchSize() {
        return commandDecodeMaxBatchSize;
    }

    public void setCommandDecodeMaxBatchSize(int commandDecodeMaxBatchSize) {
        this.commandDecodeMaxBatchSize = commandDecodeMaxBatchSize;
    }

    public int getCommandDecodeBufferInitializerSize() {
        return commandDecodeBufferInitializerSize;
    }

    public void setCommandDecodeBufferInitializerSize(int commandDecodeBufferInitializerSize) {
        this.commandDecodeBufferInitializerSize = commandDecodeBufferInitializerSize;
    }
}
