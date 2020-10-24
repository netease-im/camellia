package com.netease.nim.camellia.redis.proxy.util;

import com.lmax.disruptor.WaitStrategy;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyCallback;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyConfig;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.SlowCommandCallback;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInitUtil.class);

    public static MonitorCallback initMonitorCallback(CamelliaServerProperties serverProperties) {
        MonitorCallback monitorCallback = null;
        String monitorCallbackClassName = serverProperties.getMonitorCallbackClassName();
        if (monitorCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(monitorCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(monitorCallbackClassName);
                }
                monitorCallback = (MonitorCallback) clazz.newInstance();
                logger.info("MonitorCallback init success, class = {}", monitorCallbackClassName);
            } catch (Exception e) {
                logger.error("MonitorCallback init error, class = {}", monitorCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return monitorCallback;
    }

    public static CommandSpendTimeConfig initCommandSpendTimeConfig(CamelliaServerProperties serverProperties) {
        boolean commandSpendTimeMonitorEnable = serverProperties.isMonitorEnable() && serverProperties.isCommandSpendTimeMonitorEnable();
        if (!commandSpendTimeMonitorEnable) return null;
        long slowCommandThresholdMillisTime = serverProperties.getSlowCommandThresholdMillisTime();
        String slowCommandCallbackClassName = serverProperties.getSlowCommandCallbackClassName();
        SlowCommandCallback slowCommandCallback = null;
        if (slowCommandCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(slowCommandCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(slowCommandCallbackClassName);
                }
                slowCommandCallback = (SlowCommandCallback) clazz.newInstance();
                logger.info("SlowCommandCallback init success, class = {}", slowCommandCallbackClassName);
            } catch (Exception e) {
                logger.error("SlowCommandCallback init error, class = {}", slowCommandCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return new CommandSpendTimeConfig(slowCommandThresholdMillisTime, slowCommandCallback);
    }

    public static CommandInterceptor initCommandInterceptor(CamelliaServerProperties serverProperties) {
        String commandInterceptorClassName = serverProperties.getCommandInterceptorClassName();
        CommandInterceptor commandInterceptor = null;
        if (commandInterceptorClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(commandInterceptorClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(commandInterceptorClassName);
                }
                commandInterceptor = (CommandInterceptor) clazz.newInstance();
                logger.info("CommandInterceptor init success, class = {}", commandInterceptorClassName);
            } catch (Exception e) {
                logger.error("CommandInterceptor init error, class = {}", commandInterceptorClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return commandInterceptor;
    }

    public static void checkDisruptorWaitStrategyClassName(CamelliaTranspondProperties transpondProperties) {
        CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = transpondProperties.getRedisConf().getDisruptorConf();
        if (disruptorConf != null) {
            String waitStrategyClassName = disruptorConf.getWaitStrategyClassName();
            if (waitStrategyClassName != null) {
                try {
                    Class<?> clazz = Class.forName(waitStrategyClassName);
                    Object o = clazz.newInstance();
                    if (!(o instanceof WaitStrategy)) {
                        throw new CamelliaRedisException("not instance of com.lmax.disruptor.WaitStrategy");
                    }
                } catch (CamelliaRedisException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("CommandInterceptor init error, class = {}", waitStrategyClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
    }

    public static CommandHotKeyMonitorConfig initCommandHotKeyMonitorConfig(CamelliaServerProperties serverProperties) {
        CommandHotKeyMonitorConfig commandHotKeyMonitorConfig = null;
        CamelliaServerProperties.HotKeyMonitorConfig config = serverProperties.getHotKeyMonitorConfig();
        if (config != null && serverProperties.isHotKeyMonitorEnable()) {
            HotKeyCallback hotKeyCallback = null;
            String hotKeyCallbackClassName = config.getHotKeyCallbackClassName();
            if (hotKeyCallbackClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(hotKeyCallbackClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(hotKeyCallbackClassName);
                    }
                    hotKeyCallback = (HotKeyCallback) clazz.newInstance();
                    logger.info("HotKeyCallback init success, class = {}", hotKeyCallbackClassName);
                } catch (Exception e) {
                    logger.error("HotKeyCallback init error, class = {}", hotKeyCallbackClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
            HotKeyConfig hotKeyConfig = new HotKeyConfig(config.getCheckPeriodMillis(), config.getCheckCacheMaxCapacity(),
                    config.getCheckThreshold(), config.getMaxHotKeyCount());
            commandHotKeyMonitorConfig = new CommandHotKeyMonitorConfig(hotKeyConfig, hotKeyCallback);
        }
        return commandHotKeyMonitorConfig;
    }
}
