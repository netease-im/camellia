package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Set;

/**
 * 相关动态配置参数是从ProxyDynamicConf去拿的，ProxyDynamicConf默认会从本地配置文件中去取
 * 如果设置了本hook，那么会优先使用hook中的配置，如果hook返回了null，那么仍然以本地配置文件中的配置为准
 * 如果想要自定义，那么自定义类，继承本类，并在启动时把hook设置进来即可
 * 对于某些参数，业务调用方是存在缓存的，因此当配置产生变更时，请务必调用reload方法来更新缓存
 * Created by caojiajun on 2021/4/22
 */
public class ProxyDynamicConfHook {

    //某个redis后端连续几次连不上后触发熔断
    public Integer failCountThreshold() {
        return null;
    }

    //某个redis后端连不上触发熔断后，熔断多少ms
    public Long failBanMillis() {
        return null;
    }

    //monitor enable，当前仅当application.yml里的monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public Boolean monitorEnable() {
        return null;
    }

    //command spend time monitor enable，当前仅当application.yml里的command-spend-time-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public Boolean commandSpendTimeMonitorEnable() {
        return null;
    }

    //slow command threshold, ms
    public Long slowCommandThresholdMillisTime() {
        return null;
    }

    //hot key monitor enable，当前仅当application.yml里的hot-key-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public Boolean hotKeyMonitorEnable(Long bid, String bgroup) {
        return null;
    }

    //hot key monitor threshold
    public Long hotKeyMonitorThreshold(Long bid, String bgroup) {
        return null;
    }

    //hot key cache enable，当前仅当application.yml里的hot-key-cache-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public Boolean hotKeyCacheEnable(Long bid, String bgroup) {
        return null;
    }

    //hot key cache need cache null
    public Boolean hotKeyCacheNeedCacheNull(Long bid, String bgroup) {
        return null;
    }

    //hot key cache threshold
    public Long hotKeyCacheThreshold(Long bid, String bgroup) {
        return null;
    }

    //hot key cache key prefix，当前仅当application.yml里的hot-key-cache-key-checker-class-name设置为com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.PrefixMatchHotKeyCacheKeyChecker才有效
    public Set<String> hotKeyCacheKeyPrefix(Long bid, String bgroup) {
        return null;
    }

    //big key monitor enable，当前仅当application.yml里的big-key-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public Boolean bigKeyMonitorEnable(Long bid, String bgroup) {
        return null;
    }

    //big key monitor hash threshold
    public Integer bigKeyMonitorHashThreshold(Long bid, String bgroup) {
        return null;
    }

    //big key monitor string threshold
    public Integer bigKeyMonitorStringThreshold(Long bid, String bgroup) {
        return null;
    }

    //big key monitor set threshold
    public Integer bigKeyMonitorSetThreshold(Long bid, String bgroup) {
        return null;
    }

    //big key monitor zset threshold
    public Integer bigKeyMonitorZSetThreshold(Long bid, String bgroup) {
        return null;
    }

    //big key monitor list threshold
    public Integer bigKeyMonitorListThreshold(Long bid, String bgroup) {
        return null;
    }

    /**
     * 触发一下所有配置的重新加载
     */
    public final void invokeUpdate() {
        ProxyDynamicConf.reload();
        ProxyDynamicConf.triggerCallback();
    }
}
