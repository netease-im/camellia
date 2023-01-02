package com.netease.nim.camellia.console.model;

import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.util.LogBean;
import org.springframework.http.HttpStatus;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
public class WebResult {
    private int code;
    private String msg;
    private Object data;

    public WebResult() {
    }

    public WebResult(int code) {
        this.code = code;
    }

    public WebResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
        LogBean.get().addProps("ret",this);
    }

    public WebResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        LogBean.get().addProps("ret",this);
    }

    public static WebResult Exception(Exception exception){
        if(exception instanceof  AppException){
            return new WebResult(((AppException)exception).getCode(),((AppException)exception).getMsg());
        }
        return new WebResult(HttpStatus.INTERNAL_SERVER_ERROR.value(),exception.getMessage());
    }


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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static WebResult success() {
        return new WebResult(200, "success", null);
    }

    public static WebResult success(Object data) {
        return new WebResult(200, "success", data);
    }

    public static WebResult internalFail(String message){
        return new WebResult(500,message,null);

    }

    public static WebResult fail(int code,String message){
        return new WebResult(code,message,null);

    }
    public static WebResult fail(int code,String message,Object data){
        return new WebResult(200,message,data);
    }
}
