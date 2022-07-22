package com.netease.nim.camellia.delayqueue.common.domain;



/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgSendResponse {

    private int code;
    private String msg;
    private CamelliaDelayMsg delayMsg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public CamelliaDelayMsg getDelayMsg() {
        return delayMsg;
    }

    public void setDelayMsg(CamelliaDelayMsg delayMsg) {
        this.delayMsg = delayMsg;
    }
}
