package com.netease.nim.camellia.redis.proxy.plugin.monitor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.monitor.SlowCommandMonitor;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

/**
 * Created by caojiajun on 2022/9/13
 */
public class MonitorProxyPlugin implements ProxyPlugin {

    private long slowCommandThresholdNanoTime;
    private SlowCommandMonitorCallback slowCommandMonitorCallback;

    @Override
    public void init(ProxyBeanFactory factory) {
        reloadConf();
        ProxyDynamicConf.registerCallback(this::reloadConf);

        String slowCommandMonitorCallbackClassName = ProxyDynamicConf.getString("slow.command.monitor.callback.className", DummySlowCommandMonitorCallback.class.getName());
        slowCommandMonitorCallback = (SlowCommandMonitorCallback) factory.getBean(BeanInitUtils.parseClass(slowCommandMonitorCallbackClassName));
    }

    private void reloadConf() {
        slowCommandThresholdNanoTime = ProxyDynamicConf.getLong("slow.command.threshold.millis", 2000) * 1000000L;
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.MONITOR_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.MONITOR_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        //属于监控类plugin，因此也受isMonitorEnable控制
        if (!ProxyMonitorCollector.isMonitorEnable()) return ProxyPluginResponse.SUCCESS;
        Command command = request.getCommand();
        CommandContext commandContext = command.getCommandContext();
        CommandCountMonitor.incr(commandContext.getBid(), commandContext.getBgroup(), command.getName());
        if (ProxyMonitorCollector.isCommandSpendTimeMonitorEnable()) {
            command.initStartNanoTime();
        }
        return ProxyPluginResponse.SUCCESS;
    }

    @Override
    public ProxyPluginResponse executeReply(ProxyReply reply) {
        //属于监控类plugin，因此也受isCommandSpendTimeMonitorEnable控制
        if (!ProxyMonitorCollector.isCommandSpendTimeMonitorEnable()) return ProxyPluginResponse.SUCCESS;
        Command command = reply.getCommand();
        if (command != null) {
            CommandContext commandContext = command.getCommandContext();
            long startNanoTime = command.getStartNanoTime();
            if (startNanoTime > 0) {
                long spend = System.nanoTime() - startNanoTime;
                CommandSpendMonitor.incr(commandContext.getBid(), commandContext.getBgroup(), command.getName(), spend);
                if (spend > slowCommandThresholdNanoTime) {
                    double spendMillis = spend / 1000000.0;
                    long thresholdMillis = slowCommandThresholdNanoTime / 1000000;
                    SlowCommandMonitor.slowCommand(command, spendMillis, thresholdMillis);
                    slowCommandMonitorCallback.callback(command, reply.getReply(), spendMillis, thresholdMillis);
                }
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
