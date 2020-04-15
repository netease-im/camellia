package com.netease.nim.camellia.redis.proxy.springboot.conf;

import com.netease.nim.camellia.redis.proxy.conf.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
@ConfigurationProperties(prefix = "camellia-redis-proxy")
public class CamelliaRedisProxyProperties {

    /**
     * proxy的密码
     */
    private String password;

    /**
     * proxy的console port，会起一个console http server，可以自己实现一些自定义指令
     */
    private int consolePort = Constants.Server.consolePort;

    /**
     * 转发类型
     * sync使用jedis进行转发，支持redis、redis sentinel、redis cluster
     * async使用netty封装的client进行转发，支持redis、redis cluster
     * custom自定义，此时需要指定customCommandInvokerClassName，该类需要实现CommandInvoker接口，并且有参数为CamelliaTranspondProperties的构造方法
     */
    private Type type = Type.async;

    /**
     *
     */
    private String customCommandInvokerClassName;

    /**
     * 是否开启监控，会记录每个命令的调用次数，see @RedisMonitor
     */
    private boolean monitorEnable = Constants.Server.monitorEnable;

    /**
     * 监控时间间隔，每个间隔生成一份监控数据，会打印到日志里，也可以用户自己选择输出到其他地方，see @RedisMonitor
     */
    private int monitorIntervalSeconds = Constants.Server.monitorIntervalSeconds;

    /**
     * netty相关参数
     */
    private NettyProperties netty = new NettyProperties();

    /**
     * 转发配置
     */
    private TranspondProperties transpond;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConsolePort() {
        return consolePort;
    }

    public void setConsolePort(int consolePort) {
        this.consolePort = consolePort;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCustomCommandInvokerClassName() {
        return customCommandInvokerClassName;
    }

    public void setCustomCommandInvokerClassName(String customCommandInvokerClassName) {
        this.customCommandInvokerClassName = customCommandInvokerClassName;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }

    public NettyProperties getNetty() {
        return netty;
    }

    public void setNetty(NettyProperties netty) {
        this.netty = netty;
    }

    public TranspondProperties getTranspond() {
        return transpond;
    }

    public void setTranspond(TranspondProperties transpond) {
        this.transpond = transpond;
    }

    public static enum Type {
        sync,
        async,
        custom,
        ;
    }

}
