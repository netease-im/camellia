package com.netease.nim.camellia.dashboard.util;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.dashboard.conf.LogInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2018/5/14.
 */
public class LogBean {
    private static final ThreadLocal<LogBean> logBeanThreadLocal = new ThreadLocal<>();

    public static LogBean init() {
        LogBean logBean = logBeanThreadLocal.get();
        if (logBean == null) {
            logBean = new LogBean();
            logBeanThreadLocal.set(logBean);
        }
        logBean.code = 0;
        logBean.uri = null;
        logBean.method = null;
        logBean.ip = null;
        logBean.startTime = 0;
        logBean.spendTime = 0;
        logBean.props.clear();
        return logBean;
    }

    public static LogBean get() {
        LogBean logBean = logBeanThreadLocal.get();
        if (logBean == null) {
            return init();
        }
        return logBean;
    }

    private String uri;
    private int code;
    private String method;
    private String source;
    private long startTime;
    private long spendTime;
    private String ip;
    private Map<String, Object> props = new HashMap<>();

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getIp() {
        return ip;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setSpendTime() {
        if (this.startTime > 0) {
            this.spendTime = System.currentTimeMillis() - startTime;
        }
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public long getSpendTime() {
        return spendTime;
    }

    public void addProps(String key, Object value) {
        this.props.put(key, value);
    }

    public void addDebugProps(String key, Object value) {
        if (LogInterceptor.isDebugEnabled()) {
            this.props.put(key, value);
        }
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uri", uri);
        jsonObject.put("method", method);
        jsonObject.put("source", source);
        jsonObject.put("ip", ip);
        jsonObject.put("startTime", startTime);
        jsonObject.put("spendTime", spendTime);
        jsonObject.put("code", code);
        jsonObject.put("props", JSONObject.toJSON(props));
        return jsonObject;
    }
}
