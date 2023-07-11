package com.netease.nim.camellia.http.accelerate.proxy.core.context;

/**
 * Created by caojiajun on 2023/7/10
 */
public class LogBean {
    private String host;
    private String path;
    private String transportAddr;
    private String upstreamAddr;
    private Long startTime;
    private Long transportServerSendTime;
    private Long transportServerReceiveTime;
    private Long upstreamSendTime;
    private Long upstreamReplyTime;
    private Long endTime;
    private String traceId;
    private ErrorReason errorReason;
    private Integer code;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTransportAddr() {
        return transportAddr;
    }

    public void setTransportAddr(String transportAddr) {
        this.transportAddr = transportAddr;
    }

    public String getUpstreamAddr() {
        return upstreamAddr;
    }

    public void setUpstreamAddr(String upstreamAddr) {
        this.upstreamAddr = upstreamAddr;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getTransportServerSendTime() {
        return transportServerSendTime;
    }

    public void setTransportServerSendTime(Long transportServerSendTime) {
        this.transportServerSendTime = transportServerSendTime;
    }

    public Long getTransportServerReceiveTime() {
        return transportServerReceiveTime;
    }

    public void setTransportServerReceiveTime(Long transportServerReceiveTime) {
        this.transportServerReceiveTime = transportServerReceiveTime;
    }

    public Long getUpstreamSendTime() {
        return upstreamSendTime;
    }

    public void setUpstreamSendTime(Long upstreamSendTime) {
        this.upstreamSendTime = upstreamSendTime;
    }

    public Long getUpstreamReplyTime() {
        return upstreamReplyTime;
    }

    public void setUpstreamReplyTime(Long upstreamReplyTime) {
        this.upstreamReplyTime = upstreamReplyTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(ErrorReason errorReason) {
        this.errorReason = errorReason;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
