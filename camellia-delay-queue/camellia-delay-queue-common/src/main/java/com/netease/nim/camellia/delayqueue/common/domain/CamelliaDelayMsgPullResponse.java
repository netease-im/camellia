package com.netease.nim.camellia.delayqueue.common.domain;



import java.util.List;

/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgPullResponse {

    private int code;
    private String msg;
    private List<CamelliaDelayMsg> delayMsgList;

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

    public List<CamelliaDelayMsg> getDelayMsgList() {
        return delayMsgList;
    }

    public void setDelayMsgList(List<CamelliaDelayMsg> delayMsgList) {
        this.delayMsgList = delayMsgList;
    }
}
