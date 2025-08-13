package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResource extends Resource {

    public static final String prefix = "hbase://";

    private enum Type {
        hbase,
        lindorm,
        obkv,
        ;
    }

    private final String zk;
    private final String zkParent;

    private String userName;
    private String password;

    private Type type = Type.hbase;

    private String obkvParamUrl;
    private String obkvFullUserName;
    private String obkvPassword;
    private String obkvSysUserName;
    private String obkvSysPassword;

    private Map<String, String> configMap;

    public HBaseResource(String zk, String zkParent) {
        this(zk, zkParent, null, null);
    }

    public HBaseResource(String zk, String zkParent, String userName, String password) {
        this(zk, zkParent, userName, password, (Boolean) null);
    }

    public HBaseResource(String obkvParamUrl, String obkvFullUserName, String obkvPassword, String obkvSysUserName, String obkvSysPassword) {
        this.zk = null;
        this.zkParent = null;
        this.userName = null;
        this.password = null;
        this.type = Type.obkv;
        this.obkvParamUrl = obkvParamUrl;
        this.obkvFullUserName = obkvFullUserName;
        this.obkvPassword = obkvPassword;
        this.obkvSysUserName = obkvSysUserName;
        this.obkvSysPassword = obkvSysPassword;
        String url = prefix +
                "obkv%" + obkvParamUrl + "%" +
                "obkvFullUserName=" + obkvFullUserName + "&" +
                "obkvPassword=" + obkvPassword + "&" +
                "obkvSysUserName=" + obkvSysUserName + "&" +
                "obkvSysPassword=" + obkvSysPassword;
        setUrl(url);
    }

    public HBaseResource(String zk, String zkParent, String userName, String password, Boolean lindorm) {
        this.zk = zk;
        this.zkParent = zkParent;
        this.userName = userName;
        this.password = password;
        if (lindorm != null && lindorm) {
            this.type = Type.lindorm;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(zk).append(zkParent);
        if (userName != null || password != null) {
            builder.append("?");
        }
        if (userName != null) {
            builder.append("userName=").append(userName).append("&");
        }
        if (password != null) {
            builder.append("password=").append(password).append("&");
        }
        if (lindorm != null) {
            builder.append("lindorm=").append(lindorm).append("&");
        }
        if (builder.charAt(builder.length() - 1) == '&') {
            builder.deleteCharAt(builder.length() - 1);
        }
        setUrl(builder.toString());
    }

    public HBaseResource(String zk, String zkParent, Map<String, String> configMap) {
        this.zk = zk;
        this.zkParent = zkParent;
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(zk).append(zkParent);
        if (configMap != null && !configMap.isEmpty()) {
            TreeMap<String, String> treeMap = new TreeMap<>(configMap);
            builder.append("?");
            for (Map.Entry<String, String> entry : treeMap.entrySet()) {
                if (entry.getKey().equals("userName")) {
                    this.userName = entry.getValue();
                } else if (entry.getKey().equals("password")) {
                    this.password = entry.getValue();
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            if (builder.charAt(builder.length() - 1) == '&') {
                builder.deleteCharAt(builder.length() - 1);
            }
            this.configMap = new HashMap<>(configMap);
            this.configMap.remove("userName");
            this.configMap.remove("password");
        }
        setUrl(builder.toString());
    }

    public String getZk() {
        return zk;
    }

    public String getZkParent() {
        return zkParent;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLindorm() {
        return type == Type.lindorm;
    }

    public boolean isObkv() {
        return type == Type.obkv;
    }

    public Map<String, String> getConfigMap() {
        if (configMap == null) {
            return null;
        }
        return new HashMap<>(configMap);
    }

    public String getObkvParamUrl() {
        return obkvParamUrl;
    }

    public String getObkvFullUserName() {
        return obkvFullUserName;
    }

    public String getObkvPassword() {
        return obkvPassword;
    }

    public String getObkvSysUserName() {
        return obkvSysUserName;
    }

    public String getObkvSysPassword() {
        return obkvSysPassword;
    }
}
