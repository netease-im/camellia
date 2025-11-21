package com.netease.nim.camellia.naming.springboot;


import java.util.HashMap;
import java.util.Map;


public class CamelliaNamingBootConf {

    private Type type = Type.NACOS;
    private boolean registerEnable = false;
    private String serviceName;
    private String host;
    private int port = -1;
    private Map<String, String> config = new HashMap<>();

    public static enum Type {
        NACOS,
        ZK,
        ;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isRegisterEnable() {
        return registerEnable;
    }

    public void setRegisterEnable(boolean registerEnable) {
        this.registerEnable = registerEnable;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
